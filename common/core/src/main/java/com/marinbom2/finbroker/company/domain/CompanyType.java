package com.marinbom2.finbroker.company.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 대출 관련 등록 업체의 유형 (md/domain-design.md §5.2). 분류는 법령 원문 확인 전 초안이다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum CompanyType implements DescriptionCode {

    LENDER("대부업자"),
    LOAN_BROKER("대부중개업자"),
    LOAN_AGENT("대출모집인");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
