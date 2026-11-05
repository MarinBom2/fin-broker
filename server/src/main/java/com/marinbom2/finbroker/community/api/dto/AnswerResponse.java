package com.marinbom2.finbroker.community.api.dto;

import com.marinbom2.finbroker.community.domain.Answer;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.warning.domain.WarningType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 답변. 작성자는 역할과 "글쓴이" 여부로만 보인다 (익명) */
@Schema(description = "답변")
public record AnswerResponse(
        @Schema(description = "답변 ID", example = "1") Long answerId,
        @Schema(description = "작성 시점 역할. {code, description}") RoleType authorRole,
        @Schema(description = "글쓴이가 단 답변인지") boolean byPostAuthor,
        @Schema(description = "내가 쓴 답변인지") boolean mine,
        @Schema(description = "답변 내용") String body,
        @Schema(description = "저장 때 찾은 주의 신호") List<WarningType> warnings,
        @Schema(description = "작성 시각 (UTC)") Instant createdAt
) {

    public static AnswerResponse from(Answer answer, Long postAuthorId, Long viewerId) {
        return new AnswerResponse(answer.getId(), answer.getAuthorRole(), answer.isWrittenBy(postAuthorId),
                answer.isWrittenBy(viewerId), answer.getBody(), answer.getWarnings().stream().sorted().toList(),
                answer.getCreatedAt());
    }
}
