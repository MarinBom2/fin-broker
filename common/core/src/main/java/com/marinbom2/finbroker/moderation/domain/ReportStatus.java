package com.marinbom2.finbroker.moderation.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 신고 처리 상태 (md/domain-design.md §4.4). 처리는 대상 단위로 한 번에 한다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum ReportStatus implements DescriptionCode {

    OPEN("접수"),
    ACCEPTED("숨김 처리"),
    DISMISSED("문제없음");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
