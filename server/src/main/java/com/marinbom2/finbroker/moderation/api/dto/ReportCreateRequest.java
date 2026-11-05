package com.marinbom2.finbroker.moderation.api.dto;

import com.marinbom2.finbroker.moderation.domain.Report;
import com.marinbom2.finbroker.moderation.domain.ReportReason;
import com.marinbom2.finbroker.moderation.domain.ReportTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 신고 (FR-MD-001) */
@Schema(description = "신고 요청")
public record ReportCreateRequest(
        @Schema(description = "대상 종류 코드", example = "POST")
        @NotNull(message = "신고할 대상을 골라 주세요.") ReportTarget targetType,

        @Schema(description = "대상 ID (글 ID 또는 답변 ID)", example = "1")
        @NotNull(message = "신고할 대상을 골라 주세요.") Long targetId,

        @Schema(description = "사유 코드", example = "SCAM")
        @NotNull(message = "신고 사유를 골라 주세요.") ReportReason reason,

        @Schema(description = "덧붙일 설명 (선택)", example = "대출 전에 돈을 보내라고 유도합니다")
        @Size(max = Report.DETAIL_MAX, message = "설명은 300자까지 쓸 수 있어요.") String detail
) {
}
