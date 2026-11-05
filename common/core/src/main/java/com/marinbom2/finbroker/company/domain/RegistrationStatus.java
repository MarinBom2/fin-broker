package com.marinbom2.finbroker.company.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 등록 정보와 대조한 결과 (md/domain-design.md §4.1).
 * UNVERIFIED 는 등록 직후의 상태일 뿐 대조 결과가 아니다. 한 번 대조하면 다시 UNVERIFIED 로 돌아가지 않는다.
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum RegistrationStatus implements DescriptionCode {

    UNVERIFIED("확인 전"),
    VERIFIED("등록 확인"),
    NOT_FOUND("등록 정보 없음"),
    EXPIRED("등록 만료");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }

    /** 이 상태에서 남길 수 있는 대조 결과 (운영자 화면의 버튼). 순서는 enum 선언 순서 */
    public List<RegistrationStatus> allowedResults() {
        return Arrays.stream(values()).filter(this::canMoveTo).toList();
    }

    /** 이 상태에서 대조 결과로 옮겨 갈 수 있는 상태. 같은 상태로 다시 대조하는 것(재확인)은 허용한다 */
    public boolean canMoveTo(RegistrationStatus next) {
        Set<RegistrationStatus> allowed = switch (this) {
            case UNVERIFIED -> Set.of(VERIFIED, NOT_FOUND);
            case VERIFIED -> Set.of(VERIFIED, EXPIRED);
            case NOT_FOUND -> Set.of(VERIFIED, NOT_FOUND);
            case EXPIRED -> Set.of(VERIFIED, EXPIRED);
        };
        return allowed.contains(next);
    }
}
