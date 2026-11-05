package com.marinbom2.finbroker.company.api;

import com.marinbom2.finbroker.company.api.dto.CompanyRegisterRequest;
import com.marinbom2.finbroker.common.api.PageResponse;
import com.marinbom2.finbroker.company.api.dto.OpsCompanyResponse;
import com.marinbom2.finbroker.company.api.dto.OpsCompanySummaryResponse;
import com.marinbom2.finbroker.company.api.dto.RegistrationCheckRequest;
import com.marinbom2.finbroker.company.application.CompanyService;
import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import com.marinbom2.finbroker.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** SCR-08: 운영자 업체 목록·현황·등록·대조. 권한은 SecurityConfig 의 /api/v1/ops/** (ADMIN, SYSTEM) */
@Tag(name = "운영자: 업체", description = "업체 목록·현황, 등록, 등록 정보 대조 (SCR-08)")
@RestController
@RequestMapping("/api/v1/ops/companies")
@RequiredArgsConstructor
public class OpsCompanyController {

    private final CompanyService companyService;

    @Operation(summary = "업체 목록",
            description = "FR-CO-002. 새로 등록한 순. 상태·검색어(상호 일부, 등록번호 전체)로 거른다. recheckDue=true 면 재확인 필요 업체만 (status 무시)")
    @GetMapping
    public PageResponse<OpsCompanyResponse> list(
            @Parameter(description = "등록 확인 상태 코드. 없으면 전체") @RequestParam(required = false) RegistrationStatus status,
            @Parameter(description = "재확인 필요 업체만") @RequestParam(defaultValue = "false") boolean recheckDue,
            @Parameter(description = "상호 일부 또는 등록번호") @RequestParam(required = false)
            @Size(max = 100, message = "검색어는 100자 이하입니다.") String keyword,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1부터입니다.") int page,
            @Parameter(description = "페이지 크기 (최대 50)") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상입니다.") @Max(value = 50, message = "페이지 크기는 50 이하입니다.") int size) {
        return PageResponse.of(companyService.opsList(status, recheckDue, keyword, page, size), OpsCompanyResponse::from);
    }

    @Operation(summary = "업체 현황", description = "FR-CO-002. 상태별 업체 수와 재확인 필요 수")
    @GetMapping("/summary")
    public OpsCompanySummaryResponse summary() {
        return OpsCompanySummaryResponse.from(companyService.opsSummary());
    }

    @Operation(summary = "업체 등록",
            description = "FR-CO-002. 등록 정보에서 확인한 상호·유형·등록번호·등록기관을 넣는다. 대조 전(UNVERIFIED)으로 시작한다")
    @ApiResponse(responseCode = "409", description = "ALREADY_EXIST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpsCompanyResponse register(@Valid @RequestBody CompanyRegisterRequest request) {
        return OpsCompanyResponse.from(companyService.register(
                request.name(), request.companyType(), request.registrationNumber(), request.registrationAuthority()));
    }

    @Operation(summary = "등록 정보 대조 결과 기록",
            description = "FR-CO-002. 등록 정보와 대조한 결과(VERIFIED, NOT_FOUND, EXPIRED)를 남긴다. 전이 표는 md/domain-design.md §4.1")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_COMPANY",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "COMPANY_STATE_CONFLICT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{companyId}/registration-status")
    public OpsCompanyResponse recordCheck(@AuthenticationPrincipal Member admin, @PathVariable Long companyId,
                                          @Valid @RequestBody RegistrationCheckRequest request) {
        return OpsCompanyResponse.from(companyService.recordCheck(companyId, request.result(), admin.getId()));
    }
}
