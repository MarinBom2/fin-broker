package com.marinbom2.finbroker.moderation.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.marinbom2.finbroker.type.common.DescriptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 신고 사유 (md/domain-design.md §5.6) */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum ReportReason implements DescriptionCode {

    PROMOTION("홍보·광고"),
    SCAM("사기 의심"),
    PRIVACY("개인정보 노출"),
    ABUSE("욕설·비방"),
    RIGHTS("권리 침해"),
    OTHER("기타");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
