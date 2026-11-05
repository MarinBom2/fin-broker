package com.marinbom2.finbroker.company.api.dto;

import com.marinbom2.finbroker.company.domain.Company;
import com.marinbom2.finbroker.company.domain.CompanyType;
import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 업체 정보. 등록 정보에서 온 사실만 내보낸다 (금리·한도·홍보 문구 없음, md/domain-design.md §5.2) */
@Schema(description = "업체 정보")
public record CompanyResponse(
        @Schema(description = "업체 ID", example = "1") Long companyId,
        @Schema(description = "등록 상호", example = "가나대부") String name,
        @Schema(description = "업체 유형. {code, description}") CompanyType companyType,
        @Schema(description = "등록번호", example = "2026-서울-0001") String registrationNumber,
        @Schema(description = "등록기관", example = "서울특별시") String registrationAuthority,
        @Schema(description = "등록 확인 상태. {code, description}") RegistrationStatus registrationStatus,
        @Schema(description = "마지막 대조 시각 (UTC). 대조 전이면 null") Instant lastCheckedAt
) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getCompanyType(),
                company.getRegistrationNumber(), company.getRegistrationAuthority(),
                company.getRegistrationStatus(), company.getLastCheckedAt());
    }
}
