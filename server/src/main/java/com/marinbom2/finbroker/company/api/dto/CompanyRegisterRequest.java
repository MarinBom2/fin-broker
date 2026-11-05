package com.marinbom2.finbroker.company.api.dto;

import com.marinbom2.finbroker.company.domain.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 운영자 업체 등록 (FR-CO-002). 예시 값은 가짜다 */
@Schema(description = "업체 등록 요청 (운영자)")
public record CompanyRegisterRequest(
        @Schema(description = "등록 상호", example = "가나대부")
        @NotBlank(message = "상호를 입력해 주세요.") @Size(max = 100) String name,

        @Schema(description = "업체 유형 코드", example = "LENDER")
        @NotNull(message = "업체 유형을 골라 주세요.") CompanyType companyType,

        @Schema(description = "등록번호", example = "2026-서울-0001")
        @NotBlank(message = "등록번호를 입력해 주세요.") @Size(max = 50) String registrationNumber,

        @Schema(description = "등록기관", example = "서울특별시")
        @NotBlank(message = "등록기관을 입력해 주세요.") @Size(max = 50) String registrationAuthority
) {
}
