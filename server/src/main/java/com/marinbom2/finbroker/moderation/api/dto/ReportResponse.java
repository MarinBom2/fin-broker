package com.marinbom2.finbroker.moderation.api.dto;

import com.marinbom2.finbroker.moderation.application.ReportService.FiledReport;
import com.marinbom2.finbroker.moderation.domain.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 신고 접수 결과. 신고자에게는 처리 결과·다른 신고 수를 알려 주지 않는다 */
@Schema(description = "신고 접수 결과")
public record ReportResponse(
        @Schema(description = "신고 ID", example = "1") Long reportId,
        @Schema(description = "처리 상태. {code, description}") ReportStatus status,
        @Schema(description = "이 신고로 대상이 바로 숨겨졌는지 (신고 누적)") boolean targetHidden
) {

    public static ReportResponse from(FiledReport filed) {
        return new ReportResponse(filed.report().getId(), filed.report().getStatus(), filed.autoHidden());
    }
}
