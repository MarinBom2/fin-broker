package com.marinbom2.finbroker.community.api.dto;

import com.marinbom2.finbroker.community.domain.LoanType;
import com.marinbom2.finbroker.community.domain.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 글쓰기·수정 (FR-CM-001). 예시 값은 가짜다 */
@Schema(description = "글쓰기·수정 요청")
public record PostWriteRequest(
        @Schema(description = "글 유형 코드", example = "QUESTION")
        @NotNull(message = "글 유형을 골라 주세요.") PostType postType,

        @Schema(description = "대출 유형 코드", example = "CREDIT")
        @NotNull(message = "대출 유형을 골라 주세요.") LoanType loanType,

        @Schema(description = "글이 가리키는 업체 ID. 없으면 null", example = "1")
        Long companyId,

        @Schema(description = "제목", example = "선입금 요구하는 곳, 정상인가요?")
        @NotBlank(message = "제목을 입력해 주세요.") @Size(max = 100, message = "제목은 100자까지 쓸 수 있어요.") String title,

        @Schema(description = "본문. 연락처·주민번호·계좌번호는 쓸 수 없다", example = "대출 전에 보증금을 먼저 보내라고 하는데 괜찮은 건가요?")
        @NotBlank(message = "내용을 입력해 주세요.") @Size(max = 5000, message = "내용은 5000자까지 쓸 수 있어요.") String body
) {
}
