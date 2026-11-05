package com.marinbom2.finbroker.warning.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 피해 예방 규칙이 찾는 위험 신호 (md/domain-design.md §6). 등급과 안내 문구를 여기 한 곳에 둔다.
 * 안내 문구는 판단("불법 업체입니다")이 아니라 위험 신호 안내로 쓴다.
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum WarningType implements DescriptionCode {

    CONTACT_EXPOSURE("연락처·개인정보 노출", WarningSeverity.BLOCK,
            "전화번호, 메신저 아이디, 주민등록번호, 계좌번호는 글에 쓸 수 없습니다. 지우고 다시 올려 주세요."),
    OVER_INTEREST_CAP("법정 최고금리 초과", WarningSeverity.WARN,
            "법정 최고금리를 넘는 금리 제안은 불법 사금융일 수 있습니다. 계약하지 말고 상담 창구에 먼저 문의해 주세요."),
    UPFRONT_PAYMENT("선입금 요구", WarningSeverity.WARN,
            "대출 전에 수수료·보증금·작업비를 먼저 보내라는 요구는 대출 사기에서 자주 보이는 수법입니다."),
    ACCOUNT_HANDOVER("통장·카드 전달 요구", WarningSeverity.WARN,
            "통장·체크카드·인증 수단을 넘기면 범죄에 이용될 수 있고, 넘긴 사람도 처벌받을 수 있습니다."),
    UNREGISTERED_COMPANY("등록 확인 안 된 업체", WarningSeverity.WARN,
            "등록 정보를 확인하지 못한 업체입니다. 거래 전에 등록 여부를 직접 확인해 주세요.");

    private final String description;
    private final WarningSeverity severity;
    private final String guidance;

    @Override
    public String getDescription() {
        return this.description;
    }
}
