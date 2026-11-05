package com.marinbom2.finbroker.community.application;

import com.marinbom2.finbroker.community.domain.Answer;
import com.marinbom2.finbroker.community.domain.AnswerRepository;
import com.marinbom2.finbroker.community.domain.LoanType;
import com.marinbom2.finbroker.community.domain.Post;
import com.marinbom2.finbroker.community.domain.PostRepository;
import com.marinbom2.finbroker.community.domain.PostStatus;
import com.marinbom2.finbroker.community.domain.PostType;
import com.marinbom2.finbroker.company.domain.Company;
import com.marinbom2.finbroker.company.domain.CompanyRepository;
import com.marinbom2.finbroker.exception.BusinessException;
import com.marinbom2.finbroker.member.domain.Member;
import com.marinbom2.finbroker.type.common.ErrorCode;
import com.marinbom2.finbroker.warning.domain.WarningInspector;
import com.marinbom2.finbroker.warning.domain.WarningReport;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커뮤니티 글·답변 (FR-CM-001~003, FR-WN-002). 저장 전에 피해 예방 검사를 서버에서 다시 돌린다.
 * 화면의 미리 보기 결과는 믿지 않는다. 글 원문은 로그에 남기지 않는다 (개인정보).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService {

    /** 최신 글이 위 */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("id"));

    private final PostRepository postRepository;
    private final AnswerRepository answerRepository;
    private final CompanyRepository companyRepository;
    private final WarningInspector warningInspector;

    /**
     * FR-CM-001, FR-WN-002: 글을 쓴다. BLOCK 신호가 있으면 ContentBlockedException, WARN 신호는 글에 남긴다.
     *
     * @throws BusinessException NOT_FOUND_COMPANY 가리킨 업체가 없을 때
     */
    @Transactional
    public PostDetail write(Member author, PostType postType, LoanType loanType, Long companyId, String title, String body) {
        Company company = companyId == null ? null : companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_COMPANY));
        WarningReport report = warningInspector.inspect(WarningTarget.of(title, body, companyId));
        report.requireNotBlocked();

        Post post = postRepository.save(Post.write(author.getId(), author.getRoleType(), postType, loanType, companyId,
                title, body, report.warnTypes()));
        log.info("[community] post postId={} type={} warnings={}", post.getId(), postType, post.getWarnings());
        return new PostDetail(post, company, List.of());
    }

    /** FR-CM-002: 게시 상태 글 목록, 최신순. 유형 필터는 null 이면 전체. @param page 1부터 */
    @Transactional(readOnly = true)
    public PostPage list(PostType postType, LoanType loanType, int page, int size) {
        Page<Post> posts = postRepository.search(PostStatus.PUBLISHED, postType, loanType,
                PageRequest.of(page - 1, size, NEWEST_FIRST));
        return new PostPage(posts, companiesOf(posts.getContent()));
    }

    /** FR-CM-002: 글 상세와 답변. 숨김·삭제된 글은 없는 글로 다룬다 */
    @Transactional(readOnly = true)
    public PostDetail get(Long postId) {
        Post post = findPublished(postId);
        Company company = post.getCompanyId() == null ? null
                : companyRepository.findById(post.getCompanyId()).orElse(null);
        List<Answer> answers = answerRepository.findByPostIdAndStatusOrderByIdAsc(postId, PostStatus.PUBLISHED);
        return new PostDetail(post, company, answers);
    }

    /** FR-CM-003, FR-WN-002: 답변을 단다. 글의 답변 수도 같은 트랜잭션에서 오른다 */
    @Transactional
    public AnswerResult answer(Member author, Long postId, String body) {
        Post post = findPublished(postId);
        WarningReport report = warningInspector.inspect(WarningTarget.of(null, body, null));
        report.requireNotBlocked();

        Answer answer = answerRepository.save(
                Answer.write(post, author.getId(), author.getRoleType(), body, report.warnTypes()));
        log.info("[community] answer answerId={} postId={} warnings={}", answer.getId(), postId, answer.getWarnings());
        return new AnswerResult(answer, post.getAuthorId());
    }

    /**
     * FR-CM-001, FR-WN-002: 작성자가 글을 고친다. 고친 내용으로 피해 예방 검사를 다시 돈다.
     *
     * @throws com.marinbom2.finbroker.community.domain.exception.NotAuthorException 작성자가 아닐 때 (403)
     */
    @Transactional
    public PostDetail edit(Member editor, Long postId, PostType postType, LoanType loanType, Long companyId, String title,
                           String body) {
        Post post = findPublished(postId);
        Company company = companyId == null ? null : companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_COMPANY));
        WarningReport report = warningInspector.inspect(WarningTarget.of(title, body, companyId));
        report.requireNotBlocked();

        post.edit(editor.getId(), postType, loanType, companyId, title, body, report.warnTypes());
        postRepository.flush(); // @Version 충돌을 이 트랜잭션 안에서 드러낸다
        log.info("[community] edit postId={} warnings={}", postId, post.getWarnings());
        return new PostDetail(post, company,
                answerRepository.findByPostIdAndStatusOrderByIdAsc(postId, PostStatus.PUBLISHED));
    }

    /** FR-CM-001: 작성자가 글을 지운다. 내용은 비우고 행은 남긴다. 달린 답변은 글과 함께 보이지 않게 된다 */
    @Transactional
    public void delete(Member editor, Long postId) {
        findPublished(postId).delete(editor.getId());
        log.info("[community] delete postId={}", postId);
    }

    /** FR-CM-003, FR-WN-002: 작성자가 답변을 고친다 */
    @Transactional
    public AnswerResult editAnswer(Member editor, Long postId, Long answerId, String body) {
        Post post = findPublished(postId);
        Answer answer = findPublishedAnswer(postId, answerId);
        WarningReport report = warningInspector.inspect(WarningTarget.of(null, body, null));
        report.requireNotBlocked();

        answer.edit(editor.getId(), body, report.warnTypes());
        log.info("[community] edit answerId={} warnings={}", answerId, answer.getWarnings());
        return new AnswerResult(answer, post.getAuthorId());
    }

    /** FR-CM-003: 작성자가 답변을 지운다. 글의 답변 수도 같은 트랜잭션에서 내려간다 */
    @Transactional
    public void deleteAnswer(Member editor, Long postId, Long answerId) {
        Post post = findPublished(postId);
        findPublishedAnswer(postId, answerId).delete(editor.getId(), post);
        log.info("[community] delete answerId={} postId={}", answerId, postId);
    }

    private Answer findPublishedAnswer(Long postId, Long answerId) {
        return answerRepository.findByIdAndPostIdAndStatus(answerId, postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ANSWER));
    }

    private Post findPublished(Long postId) {
        return postRepository.findByIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST));
    }

    /** 목록에 보일 업체를 한 번에 읽는다 (글마다 따로 읽지 않게) */
    private Map<Long, Company> companiesOf(List<Post> posts) {
        List<Long> ids = posts.stream().map(Post::getCompanyId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return companyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
    }

    /** 목록 한 페이지와 그 글들이 가리키는 업체 */
    public record PostPage(Page<Post> posts, Map<Long, Company> companies) {
    }

    /** 글 상세. 업체가 없거나 지워졌으면 company 는 null */
    public record PostDetail(Post post, Company company, List<Answer> answers) {
    }

    /** 단 답변과 그 글의 작성자 (응답의 "글쓴이" 표시용) */
    public record AnswerResult(Answer answer, Long postAuthorId) {
    }
}
