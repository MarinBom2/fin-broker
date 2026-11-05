package com.marinbom2.finbroker.community.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 글 유형 (md/domain-design.md §5.4) */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum PostType implements DescriptionCode {

    QUESTION("질문"),
    REVIEW("후기"),
    SCAM_CASE("피해 사례"),
    INFO("정보");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
