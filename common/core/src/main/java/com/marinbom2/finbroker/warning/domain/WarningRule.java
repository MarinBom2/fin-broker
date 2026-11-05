package com.marinbom2.finbroker.warning.domain;

import java.util.List;

/**
 * 피해 예방 규칙 하나 (md/domain-design.md §6). 규칙을 더할 때는 구현체 하나를 빈으로 등록한다.
 *
 * 규약
 * - 위험 신호가 없으면 빈 목록을 돌려준다. 예외를 던지지 않는다
 * - 근거 조각(evidence)에 개인정보가 들어가면 가린 뒤 넣는다
 * - 판정만 한다. 저장 거부·배지 같은 결정은 등급(WarningType.severity)을 보고 호출부가 한다
 */
public interface WarningRule {

    List<Warning> inspect(WarningTarget target);
}
