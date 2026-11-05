package com.marinbom2.finbroker.warning.domain;

import com.marinbom2.finbroker.warning.domain.exception.ContentBlockedException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 규칙 전부를 돌린 결과. BLOCK 이 먼저, 같은 등급은 WarningType 순서로 정렬돼 있다 */
public record WarningReport(List<Warning> warnings) {

    public WarningReport {
        warnings = List.copyOf(warnings);
    }

    /** 저장하면 안 되는지 (BLOCK 이 하나라도 있음) */
    public boolean isBlocked() {
        return warnings.stream().anyMatch(w -> w.severity() == WarningSeverity.BLOCK);
    }

    /** 저장할 내용에 남길 주의 신호 종류. BLOCK 이 있으면 저장하지 않으므로 WARN 만 모은다 */
    public Set<WarningType> warnTypes() {
        Set<WarningType> types = EnumSet.noneOf(WarningType.class);
        warnings.stream().filter(w -> w.severity() == WarningSeverity.WARN).forEach(w -> types.add(w.type()));
        return types;
    }

    /**
     * 저장해도 되는지 확인한다.
     *
     * @throws ContentBlockedException BLOCK 신호가 있을 때. 경고 전부를 담는다
     */
    public void requireNotBlocked() {
        if (isBlocked()) {
            throw new ContentBlockedException(warnings);
        }
    }
}
