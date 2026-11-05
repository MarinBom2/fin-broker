package com.marinbom2.finbroker.community.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 글이 다루는 대출 유형 (md/domain-design.md §5.4). 분류는 초안이다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum LoanType implements DescriptionCode {

    CREDIT("신용대출"),
    SECURED("담보대출"),
    POLICY("정책서민금융"),
    BUSINESS("사업자대출"),
    OTHER("기타");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
