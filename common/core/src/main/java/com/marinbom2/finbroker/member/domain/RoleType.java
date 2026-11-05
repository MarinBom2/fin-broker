package com.marinbom2.finbroker.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 역할 (FR-AU-003). 권한 이름은 enum 이름 그대로 쓴다 (ROLE_ 접두사 없음).
 * 가입하면 USER(금융소비자). COMPANY 는 등록 확인을 마친 대출 업체(대부업자·대부중개업자·대출모집인)의 담당자만 받는다
 * (md/domain-design.md §4.1). 이 권한은 업체 정보 관리와 업체 답변 표시에만 쓰고, 고객 연결이나 노출 순위에는 쓰지 않는다
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum RoleType implements DescriptionCode {

    USER("금융소비자"),
    COMPANY("등록 업체"),
    ADMIN("관리자"),
    SYSTEM("시스템 관리자");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
