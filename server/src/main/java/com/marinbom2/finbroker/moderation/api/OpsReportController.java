package com.marinbom2.finbroker.moderation.api;

import com.marinbom2.finbroker.common.api.PageResponse;
import com.marinbom2.finbroker.member.domain.Member;
import com.marinbom2.finbroker.moderation.api.dto.OpsReportResponse;
import com.marinbom2.finbroker.moderation.api.dto.ReportDecisionRequest;
import com.marinbom2.finbroker.moderation.application.ReportService;
import com.marinbom2.finbroker.moderation.domain.ReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** SCR-08: 운영자 신고 처리. 권한은 SecurityConfig 의 /api/v1/ops/** (ADMIN, SYSTEM) */
@Tag(name = "운영자: 신고", description = "신고 목록, 숨김 처리·문제없음 (SCR-08)")
@RestController
@RequestMapping("/api/v1/ops/reports")
@RequiredArgsConstructor
public class OpsReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 목록", description = "FR-MD-002. 상태별 최신순. 대상 내용 앞부분과 같은 대상의 접수 신고 수를 함께 보여 준다")
    @GetMapping
    public PageResponse<OpsReportResponse> list(
            @Parameter(description = "처리 상태 코드. 기본 OPEN") @RequestParam(defaultValue = "OPEN") ReportStatus status,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1부터입니다.") int page,
            @Parameter(description = "페이지 크기 (최대 50)") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상입니다.") @Max(value = 50, message = "페이지 크기는 50 이하입니다.") int size) {
        return PageResponse.of(reportService.list(status, page, size), OpsReportResponse::from);
    }

    @Operation(summary = "신고 처리",
            description = "FR-MD-002. ACCEPTED 면 대상을 숨기고, DISMISSED 면 (신고 누적으로) 숨겨진 대상을 되살린다. 같은 대상의 접수 신고에 모두 적용된다")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_REPORT",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{reportId}")
    public OpsReportResponse decide(@AuthenticationPrincipal Member admin, @PathVariable Long reportId,
                                    @Valid @RequestBody ReportDecisionRequest request) {
        return OpsReportResponse.from(reportService.decide(reportId, request.decision(), admin.getId()));
    }
}
