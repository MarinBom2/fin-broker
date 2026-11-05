package com.marinbom2.finbroker.moderation.api.dto;

import com.marinbom2.finbroker.community.domain.Answer;
import com.marinbom2.finbroker.community.domain.Post;
import com.marinbom2.finbroker.community.domain.PostStatus;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.moderation.application.ReportService.ReportView;
import com.marinbom2.finbroker.moderation.domain.Report;
import com.marinbom2.finbroker.moderation.domain.ReportReason;
import com.marinbom2.finbroker.moderation.domain.ReportStatus;
import com.marinbom2.finbroker.moderation.domain.ReportTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 운영자 신고 목록의 한 줄 (FR-MD-002). 대상 하나에 대한 신고를 묶어, 사유·메모·건수와 대상 내용 앞부분을 함께 보여 준다.
 * 신고자 정보는 내보내지 않는다.
 */
@Schema(description = "대상 하나에 대한 신고 묶음 (운영자)")
public record OpsReportResponse(
        @Schema(description = "처리에 쓰는 대표 신고 ID (가장 최근 신고)", example = "1") Long reportId,
        @Schema(description = "대상 종류. {code, description}") ReportTarget targetType,
        @Schema(description = "대상 ID", example = "1") Long targetId,
        @Schema(description = "대상이 속한 글 ID", example = "1") Long postId,
        @Schema(description = "신고 사유. 많이 고른 순") List<ReportReason> reasons,
        @Schema(description = "신고자가 덧붙인 설명들. 없으면 빈 목록") List<String> details,
        @Schema(description = "이 묶음의 신고 수", example = "2") int reportCount,
        @Schema(description = "처리 상태. {code, description}") ReportStatus status,
        @Schema(description = "가장 최근 신고 시각 (UTC)") Instant lastReportedAt,
        @Schema(description = "처리 시각 (UTC). 접수 상태면 null") Instant decidedAt,
        @Schema(description = "대상 글 제목 (답변이면 그 글의 제목)") String postTitle,
        @Schema(description = "대상 내용 앞부분 (최대 120자)") String excerpt,
        @Schema(description = "대상 작성자 역할. {code, description}") RoleType authorRole,
        @Schema(description = "대상의 지금 상태. {code, description}") PostStatus targetStatus
) {

    static final int EXCERPT_LENGTH = 120;

    public static OpsReportResponse from(ReportView view) {
        Report report = view.report();
        Post post = view.post();
        Answer answer = view.answer();
        boolean isAnswer = report.getTargetType() == ReportTarget.ANSWER && answer != null;
        String body = isAnswer ? answer.getBody() : post == null ? "" : post.getBody();
        String flat = body.replaceAll("\\s+", " ");
        String excerpt = flat.length() > EXCERPT_LENGTH ? flat.substring(0, EXCERPT_LENGTH) + "…" : flat;
        List<Report> group = view.group().isEmpty() ? List.of(report) : view.group();

        return new OpsReportResponse(report.getId(), report.getTargetType(), report.getTargetId(), report.getPostId(),
                reasonsByFrequency(group), group.stream().map(Report::getDetail).filter(Objects::nonNull).toList(),
                group.size(), report.getStatus(), report.getCreatedAt(), report.getDecidedAt(),
                post == null ? null : post.getTitle(), excerpt,
                isAnswer ? answer.getAuthorRole() : post == null ? null : post.getAuthorRole(),
                isAnswer ? answer.getStatus() : post == null ? null : post.getStatus());
    }

    private static List<ReportReason> reasonsByFrequency(List<Report> group) {
        Map<ReportReason, Long> counts = group.stream()
                .collect(Collectors.groupingBy(Report::getReason, Collectors.counting()));
        return counts.keySet().stream()
                .sorted(Comparator.comparing((ReportReason r) -> counts.get(r)).reversed().thenComparing(Function.identity()))
                .toList();
    }
}
