package com.marinbom2.finbroker.company.api.dto;

import com.marinbom2.finbroker.company.application.OpsCompanySummary;
import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;

/** 운영자 업체 현황 (SCR-08). 상태는 enum 선언 순서로 모두 내려 준다 */
@Schema(description = "업체 현황 (운영자)")
public record OpsCompanySummaryResponse(
        @Schema(description = "전체 업체 수", example = "12") long total,
        @Schema(description = "상태별 업체 수") List<StatusCount> statuses,
        @Schema(description = "재확인 필요 업체 수", example = "2") long recheckDue,
        @Schema(description = "재확인 기간 (일)", example = "90") long recheckAfterDays
) {

    @Schema(description = "상태별 업체 수")
    public record StatusCount(
            @Schema(description = "등록 확인 상태. {code, description}") RegistrationStatus status,
            @Schema(description = "업체 수", example = "3") long count) {
    }

    public static OpsCompanySummaryResponse from(OpsCompanySummary summary) {
        List<StatusCount> statuses = Arrays.stream(RegistrationStatus.values())
                .map(status -> new StatusCount(status, summary.counts().getOrDefault(status, 0L)))
                .toList();
        long total = statuses.stream().mapToLong(StatusCount::count).sum();
        return new OpsCompanySummaryResponse(total, statuses, summary.recheckDue(), summary.recheckAfter().toDays());
    }
}
