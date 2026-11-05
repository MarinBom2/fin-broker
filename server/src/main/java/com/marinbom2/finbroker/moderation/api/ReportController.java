package com.marinbom2.finbroker.moderation.api;

import com.marinbom2.finbroker.member.domain.Member;
import com.marinbom2.finbroker.moderation.api.dto.ReportCreateRequest;
import com.marinbom2.finbroker.moderation.api.dto.ReportResponse;
import com.marinbom2.finbroker.moderation.application.ReportService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** SCR-03: 글·답변 신고 */
@Tag(name = "신고", description = "글·답변 신고 (SCR-03)")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고하기",
            description = "FR-MD-001. 게시 중인 글·답변을 신고한다. 한 대상은 한 번만, 내 글은 신고할 수 없다. "
                    + "서로 다른 회원의 신고가 기준(moderation.auto-hide-report-count)에 이르면 운영자 확인 전에 먼저 숨긴다")
    @ApiResponse(responseCode = "400", description = "SELF_REPORT, INVALID_REQUEST_DATA",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_POST, NOT_FOUND_ANSWER",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "ALREADY_REPORTED",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse file(@AuthenticationPrincipal Member member, @Valid @RequestBody ReportCreateRequest request) {
        return ReportResponse.from(reportService.file(member, request.targetType(), request.targetId(),
                request.reason(), request.detail()));
    }
}
