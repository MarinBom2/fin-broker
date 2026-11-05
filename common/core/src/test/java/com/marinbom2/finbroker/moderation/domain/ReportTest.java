package com.marinbom2.finbroker.moderation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("신고는 접수(OPEN)로 시작하고, 빈 설명은 null 로 둔다 (FR-MD-001)")
    void file() {
        Report report = Report.file(1L, ReportTarget.ANSWER, 7L, 3L, ReportReason.SCAM, "  ");

        assertThat(report.getStatus()).isEqualTo(ReportStatus.OPEN);
        assertThat(report.getDetail()).isNull();
        assertThat(report.getPostId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("운영자 결정은 접수 상태에서 한 번만. 이미 닫힌 신고는 바뀌지 않는다 (FR-MD-002)")
    void decideOnce() {
        Report report = Report.file(1L, ReportTarget.POST, 7L, 7L, ReportReason.PROMOTION, "광고 글");

        report.decide(ReportStatus.ACCEPTED, 9L, NOW);
        report.decide(ReportStatus.DISMISSED, 8L, NOW.plusSeconds(60));

        assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
        assertThat(report.getDecidedBy()).isEqualTo(9L);
        assertThat(report.getDecidedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("결정으로 OPEN 을 줄 수 없다")
    void openIsNotDecision() {
        Report report = Report.file(1L, ReportTarget.POST, 7L, 7L, ReportReason.OTHER, null);

        assertThatThrownBy(() -> report.decide(ReportStatus.OPEN, 9L, NOW)).isInstanceOf(IllegalArgumentException.class);
    }
}
