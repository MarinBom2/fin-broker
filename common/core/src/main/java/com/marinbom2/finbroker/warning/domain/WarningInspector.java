package com.marinbom2.finbroker.warning.domain;

import java.util.Comparator;
import java.util.List;

/**
 * 등록된 규칙 전부를 돌려 하나의 결과로 모은다. 규칙이 늘어도 이 클래스는 바뀌지 않는다 (md/solid.md O).
 * 같은 종류·같은 근거의 경고는 한 번만 남긴다.
 */
public class WarningInspector {

    private static final Comparator<Warning> ORDER = Comparator
            .comparing(Warning::severity)
            .thenComparing(Warning::type);

    private final List<WarningRule> rules;

    public WarningInspector(List<WarningRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public WarningReport inspect(WarningTarget target) {
        List<Warning> warnings = rules.stream()
                .flatMap(rule -> rule.inspect(target).stream())
                .distinct()
                .sorted(ORDER)
                .toList();
        return new WarningReport(warnings);
    }
}
