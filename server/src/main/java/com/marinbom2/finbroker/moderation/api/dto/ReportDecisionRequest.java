package com.marinbom2.finbroker.moderation.api.dto;

import com.marinbom2.finbroker.moderation.domain.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 운영자 결정 (FR-MD-002). 같은 대상의 접수 신고 전부에 적용된다 */
@Schema(description = "신고 처리 요청 (운영자)")
public record ReportDecisionRequest(
        @Schema(description = "ACCEPTED(숨김 처리) 또는 DISMISSED(문제없음)", example = "ACCEPTED")
        @NotNull(message = "처리 결과를 골라 주세요.") ReportStatus decision
) {
}
