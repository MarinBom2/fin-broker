package com.marinbom2.finbroker.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 역할 (FR-AU-003). 권한 이름은 enum 이름 그대로 쓴다 (ROLE_ 접두사 없음).
 * 가입하면 USER(금융소비자). BROKER 는 등록 확인을 마친 판매대리·중개업자만 받는다 (md/domain-design.md §5.2)
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum RoleType implements DescriptionCode {

    USER("금융소비자"),
    BROKER("판매대리·중개업자"),
    ADMIN("관리자"),
    SYSTEM("시스템 관리자");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
