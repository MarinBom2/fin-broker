package com.marinbom2.finbroker.community.api.dto;

import com.marinbom2.finbroker.company.domain.Company;
import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** 글이 가리키는 업체의 요약. 등록 확인 상태를 함께 보여 준다 */
@Schema(description = "글이 가리키는 업체")
public record CompanyRef(
        @Schema(description = "업체 ID", example = "1") Long companyId,
        @Schema(description = "등록 상호", example = "가나대부") String name,
        @Schema(description = "등록 확인 상태. {code, description}") RegistrationStatus registrationStatus
) {

    /** @return 업체가 없으면 null */
    public static CompanyRef from(Company company) {
        return company == null ? null
                : new CompanyRef(company.getId(), company.getName(), company.getRegistrationStatus());
    }
}
