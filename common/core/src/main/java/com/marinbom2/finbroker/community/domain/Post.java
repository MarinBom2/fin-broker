package com.marinbom2.finbroker.community.domain;

import com.marinbom2.finbroker.community.domain.exception.NotAuthorException;
import com.marinbom2.finbroker.domain.base.BaseEntity;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.warning.domain.WarningType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커뮤니티 글 (FR-CM-001·002, md/domain-design.md §5.4). 작성자는 화면에 역할로만 보인다 (익명).
 * 저장 전 피해 예방 검사를 통과해야 하고, 주의(WARN) 신호는 글에 남겨 배지로 보여 준다 (§4.2).
 */
@Getter
@Entity
@Table(name = "post", indexes = {
        @Index(name = "idx_post_status_type", columnList = "status, post_type, id"),
        @Index(name = "idx_post_company", columnList = "company_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 작성 시점 역할. 나중에 역할이 바뀌어도 당시 "등록 업체 작성"이었다는 사실은 남긴다 */
    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 20)
    private RoleType authorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    /** 후기·피해 사례가 가리키는 업체. 없으면 null */
    @Column(name = "company_id")
    private Long companyId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    /** 목록 표시용. 답변 추가와 같은 트랜잭션에서 바꾼다 */
    @Column(name = "answer_count", nullable = false)
    private int answerCount;

    /** 저장 시 찾은 주의 신호 (WARN 만. BLOCK 이 있으면 저장되지 않는다) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_warning", joinColumns = @JoinColumn(name = "post_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 30)
    private Set<WarningType> warnings = EnumSet.noneOf(WarningType.class);

    @Version
    private Long version;

    public static Post write(Long authorId, RoleType authorRole, PostType postType, LoanType loanType, Long companyId,
                             String title, String body, Set<WarningType> warnings) {
        Post post = new Post();
        post.authorId = authorId;
        post.authorRole = authorRole;
        post.postType = postType;
        post.loanType = loanType;
        post.companyId = companyId;
        post.title = title.strip();
        post.body = body.strip();
        post.status = PostStatus.PUBLISHED;
        post.answerCount = 0;
        post.warnings = warnings.isEmpty() ? EnumSet.noneOf(WarningType.class) : EnumSet.copyOf(warnings);
        return post;
    }

    /**
     * 작성자가 글을 고친다 (FR-CM-001). 주의 신호는 고친 내용으로 다시 검사한 결과로 바꾼다.
     *
     * @throws NotAuthorException 작성자가 아닐 때
     */
    public void edit(Long memberId, PostType postType, LoanType loanType, Long companyId, String title, String body,
                     Set<WarningType> warnings) {
        requireAuthor(memberId);
        requirePublished();
        this.postType = postType;
        this.loanType = loanType;
        this.companyId = companyId;
        this.title = title.strip();
        this.body = body.strip();
        replaceWarnings(warnings);
    }

    /**
     * 작성자가 글을 지운다 (FR-CM-001). 행은 남기고 내용은 비운다 (개인정보·다른 회원의 답변 관계 때문에 행을 지우지 않는다).
     *
     * @throws NotAuthorException 작성자가 아닐 때
     */
    public void delete(Long memberId) {
        requireAuthor(memberId);
        requirePublished();
        this.status = PostStatus.DELETED;
        this.title = "";
        this.body = "";
        this.companyId = null;
        this.warnings.clear();
    }

    /** 운영자 숨김·신고 누적 (FR-MD-002). 게시 상태일 때만 바뀐다 (이미 숨김·삭제면 그대로) */
    public void hide() {
        if (status == PostStatus.PUBLISHED) {
            this.status = PostStatus.HIDDEN;
        }
    }

    /** 운영자가 신고를 "문제없음"으로 처리하면 숨김을 푼다. 숨김 상태일 때만 바뀐다 */
    public void restore() {
        if (status == PostStatus.HIDDEN) {
            this.status = PostStatus.PUBLISHED;
        }
    }

    /** 답변이 하나 늘었다. 답변 저장과 같은 트랜잭션에서 부른다 (@Version 으로 동시 갱신을 막는다) */
    public void increaseAnswerCount() {
        this.answerCount++;
    }

    /** 게시 중인 답변이 하나 줄었다 (삭제·숨김). 0 아래로 내려가지 않는다 */
    public void decreaseAnswerCount() {
        if (this.answerCount > 0) {
            this.answerCount--;
        }
    }

    private void requireAuthor(Long memberId) {
        if (!isWrittenBy(memberId)) {
            throw new NotAuthorException("post", id, memberId);
        }
    }

    /** 서비스는 게시 상태 글만 읽어 넘긴다. 여기까지 오면 호출부 버그다 */
    private void requirePublished() {
        if (!isPublished()) {
            throw new IllegalStateException("게시 상태가 아닌 글: " + id);
        }
    }

    private void replaceWarnings(Set<WarningType> next) {
        this.warnings.clear();
        this.warnings.addAll(next);
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    public boolean isWrittenBy(Long memberId) {
        return authorId.equals(memberId);
    }
}
