package com.marinbom2.finbroker.member.api.dto;

import com.marinbom2.finbroker.member.domain.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "내 권한 바꾸기 요청 (local 전용)")
public record RoleChangeRequest(
        @Schema(description = "바꿀 권한 코드", example = "ADMIN") @NotNull RoleType roleType
) {
}
