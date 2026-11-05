package com.marinbom2.finbroker.moderation.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 신고 대상 종류 (md/domain-design.md §5.6) */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum ReportTarget implements DescriptionCode {

    POST("글"),
    ANSWER("답변");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
