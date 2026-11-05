package com.marinbom2.finbroker.member.api;

import com.marinbom2.finbroker.member.api.dto.MemberResponse;
import com.marinbom2.finbroker.member.api.dto.RoleChangeRequest;
import com.marinbom2.finbroker.member.application.MemberService;
import com.marinbom2.finbroker.member.domain.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 회원 본인의 정보 */
@Tag(name = "내 정보", description = "세션 확인, 내 정보, 권한 (SCR-07)")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final MemberService memberService;

    @Operation(summary = "내 정보",
            description = "FR-AU-002. 로그인 상태 확인에도 쓴다. 인증 쿠키가 없거나 무효면 401")
    @GetMapping
    public MemberResponse me(@AuthenticationPrincipal Member member) {
        return MemberResponse.from(memberService.get(member.getId()));
    }

    @Operation(summary = "내 권한 바꾸기 (local 전용)",
            description = "FR-AU-003. 업체·운영자 화면을 로컬에서 확인할 때 쓴다. security.role.self-change-allowed 가 꺼진 환경(운영)에서는 403")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/role")
    public MemberResponse changeRole(@AuthenticationPrincipal Member member, @Valid @RequestBody RoleChangeRequest request) {
        return MemberResponse.from(memberService.changeOwnRole(member.getId(), request.roleType()));
    }
}
