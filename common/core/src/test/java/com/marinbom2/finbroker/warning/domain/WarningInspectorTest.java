package com.marinbom2.finbroker.warning.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WarningInspectorTest {

    private static final WarningRule WARN_RULE = target -> List.of(new Warning(WarningType.UPFRONT_PAYMENT, "선입금"));
    private static final WarningRule BLOCK_RULE = target -> List.of(new Warning(WarningType.CONTACT_EXPOSURE, "***-****-5678"));
    private static final WarningRule EMPTY_RULE = target -> List.of();

    @Test
    @DisplayName("규칙 결과를 모으고 BLOCK 을 먼저 둔다. BLOCK 이 있으면 저장 불가")
    void collectsAndOrders() {
        WarningReport report = new WarningInspector(List.of(WARN_RULE, EMPTY_RULE, BLOCK_RULE))
                .inspect(new WarningTarget("text", null));

        assertThat(report.warnings()).extracting(Warning::type)
                .containsExactly(WarningType.CONTACT_EXPOSURE, WarningType.UPFRONT_PAYMENT);
        assertThat(report.isBlocked()).isTrue();
    }

    @Test
    @DisplayName("WARN 만 있으면 저장할 수 있다. 같은 종류·근거는 한 번만")
    void warnOnlyAndDistinct() {
        WarningReport report = new WarningInspector(List.of(WARN_RULE, WARN_RULE))
                .inspect(new WarningTarget("text", null));

        assertThat(report.warnings()).hasSize(1);
        assertThat(report.isBlocked()).isFalse();
    }

    @Test
    @DisplayName("근거 조각은 40자에서 자른다")
    void truncatesEvidence() {
        Warning warning = new Warning(WarningType.UPFRONT_PAYMENT, "가".repeat(50));

        assertThat(warning.evidence()).hasSize(Warning.MAX_EVIDENCE_LENGTH + 1).endsWith("…");
    }
}
