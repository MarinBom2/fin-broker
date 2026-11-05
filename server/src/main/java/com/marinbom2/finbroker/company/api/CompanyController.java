package com.marinbom2.finbroker.company.api;

import com.marinbom2.finbroker.common.api.PageResponse;
import com.marinbom2.finbroker.company.api.dto.CompanyResponse;
import com.marinbom2.finbroker.company.application.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** SCR-05: 업체 조회. 등록 정보와 확인 상태만 보여 준다. 순서·노출은 돈으로 바뀌지 않는다 */
@Tag(name = "업체 조회", description = "등록 정보·확인 상태 조회 (SCR-05)")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "업체 검색",
            description = "FR-CO-001. 상호 일부 또는 등록번호 전체로 찾는다. 상호 가나다순. 검색어가 없으면 전체")
    @GetMapping
    public PageResponse<CompanyResponse> search(
            @Parameter(description = "상호 일부 또는 등록번호") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1부터입니다.") int page,
            @Parameter(description = "페이지 크기 (최대 50)") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상입니다.") @Max(value = 50, message = "페이지 크기는 50 이하입니다.") int size) {
        return PageResponse.of(companyService.search(keyword, page, size), CompanyResponse::from);
    }

    @Operation(summary = "업체 상세", description = "FR-CO-001. 등록 정보와 확인 상태")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_COMPANY",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{companyId}")
    public CompanyResponse get(@PathVariable Long companyId) {
        return CompanyResponse.from(companyService.get(companyId));
    }
}
