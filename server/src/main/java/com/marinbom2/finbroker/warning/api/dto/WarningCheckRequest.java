package com.marinbom2.finbroker.warning.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 검사할 문장 (FR-WN-001). 글쓰기 미리 보기, 받은 문자 붙여 넣기 둘 다 이 형태다. 저장하지 않는다 */
@Schema(description = "피해 예방 검사 요청")
public record WarningCheckRequest(
        @Schema(description = "제목. 없으면 비운다", example = "대출 문의")
        @Size(max = 200) String title,

        @Schema(description = "본문 또는 받은 문자 내용", example = "선입금 30만원 보내 주시면 연 30%로 바로 승인됩니다")
        @NotBlank(message = "검사할 내용을 입력해 주세요.") @Size(max = 5000, message = "5000자까지 검사할 수 있습니다.") String body,

        @Schema(description = "내용이 가리키는 업체 ID. 없으면 null", example = "1")
        Long companyId
) {
}
