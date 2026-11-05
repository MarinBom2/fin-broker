package com.marinbom2.finbroker.warning.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 경고 등급. BLOCK 이 하나라도 있으면 글·답변을 저장하지 않는다 (md/domain-design.md §4.2) */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum WarningSeverity implements DescriptionCode {

    BLOCK("등록할 수 없음"),
    WARN("주의");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
