package com.marinbom2.finbroker.moderation.application;

import com.marinbom2.finbroker.community.domain.Answer;
import com.marinbom2.finbroker.community.domain.AnswerRepository;
import com.marinbom2.finbroker.community.domain.Post;
import com.marinbom2.finbroker.community.domain.PostRepository;
import com.marinbom2.finbroker.community.domain.PostStatus;
import com.marinbom2.finbroker.exception.BusinessException;
import com.marinbom2.finbroker.member.domain.Member;
import com.marinbom2.finbroker.moderation.domain.Report;
import com.marinbom2.finbroker.moderation.domain.ReportReason;
import com.marinbom2.finbroker.moderation.domain.ReportRepository;
import com.marinbom2.finbroker.moderation.domain.ReportStatus;
import com.marinbom2.finbroker.moderation.domain.ReportTarget;
import com.marinbom2.finbroker.type.common.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 (FR-MD-001)와 운영자 처리 (FR-MD-002). 처리는 대상 단위다: 같은 대상의 접수 신고를 한 번에 닫고,
 * 숨김 처리면 대상을 숨기고, 문제없음이면 (신고 누적으로) 숨겨진 대상을 되살린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(ModerationProperties.class)
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final AnswerRepository answerRepository;
    private final ModerationProperties properties;
    private final Clock clock;

    /**
     * FR-MD-001: 게시 중인 글·답변을 신고한다. 서로 다른 회원의 접수 신고가 기준에 이르면 바로 숨긴다.
     *
     * @throws BusinessException NOT_FOUND_POST·NOT_FOUND_ANSWER(대상 없음), SELF_REPORT(내 글), ALREADY_REPORTED(중복)
     */
    @Transactional
    public FiledReport file(Member reporter, ReportTarget targetType, Long targetId, ReportReason reason, String detail) {
        Target target = loadPublished(targetType, targetId);
        if (target.authorId().equals(reporter.getId())) {
            throw new BusinessException(ErrorCode.SELF_REPORT);
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporter.getId(), targetType, targetId)) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }

        Report report;
        try {
            report = reportRepository.saveAndFlush(
                    Report.file(reporter.getId(), targetType, targetId, target.post().getId(), reason, detail));
        } catch (DataIntegrityViolationException e) {
            // 같은 회원이 동시에 두 번 누르면 위 확인을 둘 다 통과할 수 있다. 유니크 제약이 최종 판정
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }

        long openCount = reportRepository.countByTargetTypeAndTargetIdAndStatus(targetType, targetId, ReportStatus.OPEN);
        boolean autoHidden = openCount >= properties.autoHideReportCount();
        if (autoHidden) {
            target.hide();
            log.info("[moderation] auto hide {} {} openReports={}", targetType, targetId, openCount);
        }
        log.info("[moderation] report reportId={} target={} {} reason={}", report.getId(), targetType, targetId, reason);
        return new FiledReport(report, autoHidden);
    }

    /**
     * FR-MD-002: 운영자 목록. 대상 하나당 한 줄 (같은 상태의 신고를 묶는다), 최근 신고가 위.
     * 대상 글·답변은 한 번에 읽는다.
     */
    @Transactional(readOnly = true)
    public Page<ReportView> list(ReportStatus status, int page, int size) {
        Page<Long> latestIds = reportRepository.findLatestIdPerTarget(status, PageRequest.of(page - 1, size));
        Map<Long, Report> latest = reportRepository.findAllById(latestIds.getContent()).stream()
                .collect(Collectors.toMap(Report::getId, Function.identity()));
        List<Report> reports = latestIds.getContent().stream().map(latest::get).toList();

        Set<Long> postIds = reports.stream().map(Report::getPostId).collect(Collectors.toSet());
        Set<Long> answerIds = reports.stream()
                .filter(r -> r.getTargetType() == ReportTarget.ANSWER).map(Report::getTargetId).collect(Collectors.toSet());
        Map<Long, Post> posts = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        Map<Long, Answer> answers = answerRepository.findAllById(answerIds).stream()
                .collect(Collectors.toMap(Answer::getId, Function.identity()));

        return latestIds.map(id -> {
            Report r = latest.get(id);
            return new ReportView(r, posts.get(r.getPostId()),
                    r.getTargetType() == ReportTarget.ANSWER ? answers.get(r.getTargetId()) : null, groupOf(r, status));
        });
    }

    /**
     * FR-MD-002: 운영자 결정. 같은 대상의 접수 신고 전부에 같은 결정을 남기고 대상을 숨기거나 되살린다.
     *
     * @param decision ACCEPTED(숨김 처리) 또는 DISMISSED(문제없음)
     */
    @Transactional
    public ReportView decide(Long reportId, ReportStatus decision, Long adminId) {
        if (decision == ReportStatus.OPEN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA, "처리 결과는 숨김 처리 또는 문제없음이어야 합니다.");
        }
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_REPORT));
        Target target = loadAny(report.getTargetType(), report.getTargetId());
        if (report.getStatus() != ReportStatus.OPEN) {
            return target.view(report, groupOf(report, report.getStatus())); // 이미 같은 대상 처리 때 함께 닫혔다
        }

        List<Report> open = reportRepository.findByTargetTypeAndTargetIdAndStatus(
                report.getTargetType(), report.getTargetId(), ReportStatus.OPEN);
        open.forEach(r -> r.decide(decision, adminId, clock.instant()));

        if (decision == ReportStatus.ACCEPTED) {
            target.hide();
        } else {
            target.restore();
        }
        log.info("[moderation] decide target={} {} decision={} reports={} by={}",
                report.getTargetType(), report.getTargetId(), decision, open.size(), adminId);
        return target.view(report, open);
    }

    /** 이 글과 답변 중 내가 신고한 대상 (화면의 "신고함" 표시) */
    @Transactional(readOnly = true)
    public Set<TargetKey> reportedBy(Long memberId, Long postId) {
        return reportRepository.findByReporterIdAndPostId(memberId, postId).stream()
                .map(r -> new TargetKey(r.getTargetType(), r.getTargetId()))
                .collect(Collectors.toSet());
    }

    private Target loadPublished(ReportTarget type, Long id) {
        return switch (type) {
            case POST -> {
                Post post = postRepository.findByIdAndStatus(id, PostStatus.PUBLISHED)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST));
                yield new Target(post, null);
            }
            case ANSWER -> {
                Answer answer = answerRepository.findById(id).filter(Answer::isPublished)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ANSWER));
                Post post = postRepository.findByIdAndStatus(answer.getPostId(), PostStatus.PUBLISHED)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST));
                yield new Target(post, answer);
            }
        };
    }

    private Target loadAny(ReportTarget type, Long id) {
        return switch (type) {
            case POST -> new Target(postRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST)), null);
            case ANSWER -> {
                Answer answer = answerRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ANSWER));
                yield new Target(postRepository.findById(answer.getPostId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_POST)), answer);
            }
        };
    }

    /** 신고 대상. answer 가 null 이면 글 자체 */
    private record Target(Post post, Answer answer) {

        Long authorId() {
            return answer == null ? post.getAuthorId() : answer.getAuthorId();
        }

        void hide() {
            if (answer == null) {
                post.hide();
            } else {
                answer.hide(post);
            }
        }

        void restore() {
            if (answer == null) {
                post.restore();
            } else {
                answer.restore(post);
            }
        }

        ReportView view(Report report, List<Report> group) {
            return new ReportView(report, post, answer, group);
        }
    }

    /** 같은 대상·같은 상태의 신고 묶음 */
    private List<Report> groupOf(Report report, ReportStatus status) {
        return reportRepository.findByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), status);
    }

    /** 신고 결과. autoHidden 이면 이 신고로 기준에 이르러 대상이 숨겨졌다 */
    public record FiledReport(Report report, boolean autoHidden) {
    }

    public record TargetKey(ReportTarget type, Long id) {
    }

    /**
     * 운영자 화면의 한 줄: 대상 하나와 그 대상에 대한 신고 묶음.
     *
     * @param report 묶음의 대표 (가장 최근 신고). 처리할 때 이 ID 를 쓴다
     * @param post   대상 글 (답변 신고면 그 답변이 달린 글). 지워진 행은 없을 수 있다
     * @param answer 답변 신고일 때만. 글 신고면 null
     * @param group  같은 대상·같은 상태의 신고 전부 (대표 포함)
     */
    public record ReportView(Report report, Post post, Answer answer, List<Report> group) {
    }
}
