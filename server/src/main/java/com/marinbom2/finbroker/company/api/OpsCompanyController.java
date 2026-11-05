package com.marinbom2.finbroker.company.api;

import com.marinbom2.finbroker.company.api.dto.CompanyRegisterRequest;
import com.marinbom2.finbroker.company.api.dto.CompanyResponse;
import com.marinbom2.finbroker.company.api.dto.RegistrationCheckRequest;
import com.marinbom2.finbroker.company.application.CompanyService;
import com.marinbom2.finbroker.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** SCR-08: 운영자 업체 등록·대조. 권한은 SecurityConfig 의 /api/v1/ops/** (ADMIN, SYSTEM) */
@Tag(name = "운영자: 업체", description = "업체 등록, 등록 정보 대조 (SCR-08)")
@RestController
@RequestMapping("/api/v1/ops/companies")
@RequiredArgsConstructor
public class OpsCompanyController {

    private final CompanyService companyService;

    @Operation(summary = "업체 등록",
            description = "FR-CO-002. 등록 정보에서 확인한 상호·유형·등록번호·등록기관을 넣는다. 대조 전(UNVERIFIED)으로 시작한다")
    @ApiResponse(responseCode = "409", description = "ALREADY_EXIST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse register(@Valid @RequestBody CompanyRegisterRequest request) {
        return CompanyResponse.from(companyService.register(
                request.name(), request.companyType(), request.registrationNumber(), request.registrationAuthority()));
    }

    @Operation(summary = "등록 정보 대조 결과 기록",
            description = "FR-CO-002. 등록 정보와 대조한 결과(VERIFIED, NOT_FOUND, EXPIRED)를 남긴다. 전이 표는 md/domain-design.md §4.1")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_COMPANY",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "COMPANY_STATE_CONFLICT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{companyId}/registration-status")
    public CompanyResponse recordCheck(@AuthenticationPrincipal Member admin, @PathVariable Long companyId,
                                       @Valid @RequestBody RegistrationCheckRequest request) {
        return CompanyResponse.from(companyService.recordCheck(companyId, request.result(), admin.getId()));
    }
}
