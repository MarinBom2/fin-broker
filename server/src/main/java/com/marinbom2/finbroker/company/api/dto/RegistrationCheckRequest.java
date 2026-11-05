package com.marinbom2.finbroker.company.api.dto;

import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 운영자가 등록 정보와 대조한 결과 (FR-CO-002) */
@Schema(description = "등록 대조 결과 (운영자)")
public record RegistrationCheckRequest(
        @Schema(description = "대조 결과 코드. VERIFIED, NOT_FOUND, EXPIRED", example = "VERIFIED")
        @NotNull(message = "대조 결과를 골라 주세요.") RegistrationStatus result
) {
}
