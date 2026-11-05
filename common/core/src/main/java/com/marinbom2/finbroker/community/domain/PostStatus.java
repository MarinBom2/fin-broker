package com.marinbom2.finbroker.community.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 글·답변 상태 (md/domain-design.md §4.4). 숨김·삭제된 글은 사용자 API 에서 없는 글로 다룬다 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum PostStatus implements DescriptionCode {

    PUBLISHED("게시"),
    HIDDEN("숨김"),
    DELETED("삭제");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
