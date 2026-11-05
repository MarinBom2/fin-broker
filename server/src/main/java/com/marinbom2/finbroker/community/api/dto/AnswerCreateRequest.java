package com.marinbom2.finbroker.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 답변 달기 (FR-CM-003) */
@Schema(description = "답변 요청")
public record AnswerCreateRequest(
        @Schema(description = "답변 내용. 연락처는 쓸 수 없다", example = "대출 전에 돈을 요구하면 사기를 의심해 보세요.")
        @NotBlank(message = "답변을 입력해 주세요.") @Size(max = 3000, message = "답변은 3000자까지 쓸 수 있어요.") String body
) {
}
