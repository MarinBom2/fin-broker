package com.marinbom2.finbroker.warning.api.dto;

import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningReport;
import com.marinbom2.finbroker.warning.domain.WarningType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 피해 예방 검사 결과 (FR-WN-001) */
@Schema(description = "피해 예방 검사 결과")
public record WarningCheckResponse(
        @Schema(description = "글로 올릴 수 없는 내용(BLOCK)이 있는지") boolean blocked,
        @Schema(description = "찾은 위험 신호. BLOCK 이 먼저 온다. 없으면 빈 목록") List<WarningItem> warnings
) {

    public static WarningCheckResponse from(WarningReport report) {
        return new WarningCheckResponse(report.isBlocked(),
                report.warnings().stream().map(WarningItem::from).toList());
    }

    /**
     * @param type     {code, description, severity: {code, description}, guidance}
     * @param evidence 근거가 된 원문 조각. 개인정보는 가려져 있다
     */
    @Schema(description = "위험 신호 하나")
    public record WarningItem(
            @Schema(description = "위험 신호 종류. 등급(severity)과 안내 문구(guidance)를 함께 담는다") WarningType type,
            @Schema(description = "근거 조각 (개인정보는 가림)", example = "선입금") String evidence
    ) {

        static WarningItem from(Warning warning) {
            return new WarningItem(warning.type(), warning.evidence());
        }
    }
}
