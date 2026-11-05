package com.marinbom2.finbroker.moderation.domain;

import com.marinbom2.finbroker.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 글·답변 신고 (FR-MD-001·002, md/domain-design.md §5.6). 한 회원은 같은 대상을 한 번만 신고한다 (유니크 제약).
 * 운영자 처리는 대상 단위다: 같은 대상의 접수 신고를 한 번에 숨김 처리·문제없음으로 닫는다.
 */
@Getter
@Entity
@Table(name = "report",
        uniqueConstraints = @UniqueConstraint(name = "uk_report_reporter_target",
                columnNames = {"reporter_id", "target_type", "target_id"}),
        indexes = {
                @Index(name = "idx_report_status", columnList = "status, id"),
                @Index(name = "idx_report_target", columnList = "target_type, target_id, status")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    /** 상세 설명 최대 길이 */
    public static final int DETAIL_MAX = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTarget targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** 답변 신고면 그 답변이 달린 글. 운영자 화면에서 글로 바로 가기 위해 둔다 */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReason reason;

    /** 신고자가 덧붙인 설명. 없으면 null */
    @Column(length = DETAIL_MAX)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by")
    private Long decidedBy;

    public static Report file(Long reporterId, ReportTarget targetType, Long targetId, Long postId, ReportReason reason,
                              String detail) {
        Report report = new Report();
        report.reporterId = reporterId;
        report.targetType = targetType;
        report.targetId = targetId;
        report.postId = postId;
        report.reason = reason;
        report.detail = detail == null || detail.isBlank() ? null : detail.strip();
        report.status = ReportStatus.OPEN;
        return report;
    }

    /**
     * 운영자 결정. 접수 상태에서만 닫는다 (이미 닫힌 신고는 그대로 둔다).
     *
     * @param decision ACCEPTED 또는 DISMISSED
     */
    public void decide(ReportStatus decision, Long adminId, Instant now) {
        if (decision == ReportStatus.OPEN) {
            throw new IllegalArgumentException("결정은 ACCEPTED 또는 DISMISSED 여야 합니다");
        }
        if (status != ReportStatus.OPEN) {
            return;
        }
        this.status = decision;
        this.decidedAt = now;
        this.decidedBy = adminId;
    }
}
