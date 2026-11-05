package com.marinbom2.finbroker.warning.domain;

import java.util.List;

/** 규칙 전부를 돌린 결과. BLOCK 이 먼저, 같은 등급은 WarningType 순서로 정렬돼 있다 */
public record WarningReport(List<Warning> warnings) {

    public WarningReport {
        warnings = List.copyOf(warnings);
    }

    /** 저장하면 안 되는지 (BLOCK 이 하나라도 있음) */
    public boolean isBlocked() {
        return warnings.stream().anyMatch(w -> w.severity() == WarningSeverity.BLOCK);
    }
}
