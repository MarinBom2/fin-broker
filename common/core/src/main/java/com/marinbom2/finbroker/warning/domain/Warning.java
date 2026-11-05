package com.marinbom2.finbroker.warning.domain;

/**
 * 규칙 하나가 찾은 위험 신호 하나.
 *
 * @param type     위험 신호 종류. 등급·안내 문구는 type 이 정한다
 * @param evidence 근거가 된 원문 조각. 개인정보는 규칙이 가린 뒤 넣는다
 */
public record Warning(WarningType type, String evidence) {

    /** 근거 조각은 이 길이까지만 보여 준다 */
    static final int MAX_EVIDENCE_LENGTH = 40;

    public Warning {
        evidence = evidence == null ? "" : evidence.strip();
        if (evidence.length() > MAX_EVIDENCE_LENGTH) {
            evidence = evidence.substring(0, MAX_EVIDENCE_LENGTH) + "…";
        }
    }

    public WarningSeverity severity() {
        return type.getSeverity();
    }
}
