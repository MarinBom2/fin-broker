package com.marinbom2.finbroker.warning.api;

import com.marinbom2.finbroker.warning.api.dto.WarningCheckRequest;
import com.marinbom2.finbroker.warning.api.dto.WarningCheckResponse;
import com.marinbom2.finbroker.warning.application.WarningCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SCR-04·SCR-06: 피해 예방 검사. 글쓰기 미리 보기와 "받은 문자 검사해 보기"가 같이 쓴다 */
@Tag(name = "피해 예방", description = "위험 신호 검사 (SCR-04, SCR-06)")
@RestController
@RequestMapping("/api/v1/warnings")
@RequiredArgsConstructor
public class WarningController {

    private final WarningCheckService warningCheckService;

    @Operation(summary = "위험 신호 검사",
            description = "FR-WN-001. 법정 최고금리 초과, 선입금 요구, 통장·카드 전달 요구, 연락처 노출, 등록 확인 안 된 업체를 찾는다. "
                    + "조회 동작이라 저장하지 않는다. 본문이 커서 POST 로 받는다")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_COMPANY",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/check")
    public WarningCheckResponse check(@Valid @RequestBody WarningCheckRequest request) {
        return WarningCheckResponse.from(warningCheckService.check(request.title(), request.body(), request.companyId()));
    }
}
