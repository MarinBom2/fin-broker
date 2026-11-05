package com.marinbom2.finbroker.company.api.dto;

import com.marinbom2.finbroker.company.application.OpsCompany;
import com.marinbom2.finbroker.company.domain.Company;
import com.marinbom2.finbroker.company.domain.CompanyType;
import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 운영자 화면의 업체 (SCR-08). 공개 업체 정보에 대조 버튼·재확인 표시용 값을 더한다 */
@Schema(description = "업체 정보 (운영자)")
public record OpsCompanyResponse(
        @Schema(description = "업체 ID", example = "1") Long companyId,
        @Schema(description = "등록 상호", example = "가나대부") String name,
        @Schema(description = "업체 유형. {code, description}") CompanyType companyType,
        @Schema(description = "등록번호", example = "2026-서울-0001") String registrationNumber,
        @Schema(description = "등록기관", example = "서울특별시") String registrationAuthority,
        @Schema(description = "등록 확인 상태. {code, description}") RegistrationStatus registrationStatus,
        @Schema(description = "마지막 대조 시각 (UTC). 대조 전이면 null") Instant lastCheckedAt,
        @Schema(description = "등록 시각 (UTC)") Instant createdAt,
        @Schema(description = "지금 상태에서 남길 수 있는 대조 결과. 화면 버튼") List<RegistrationStatus> allowedResults,
        @Schema(description = "등록 확인한 지 재확인 기간이 지났는지") boolean recheckDue
) {

    public static OpsCompanyResponse from(OpsCompany view) {
        Company company = view.company();
        return new OpsCompanyResponse(company.getId(), company.getName(), company.getCompanyType(),
                company.getRegistrationNumber(), company.getRegistrationAuthority(), company.getRegistrationStatus(),
                company.getLastCheckedAt(), company.getCreatedAt(),
                company.getRegistrationStatus().allowedResults(), view.recheckDue());
    }
}
