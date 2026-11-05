package com.marinbom2.finbroker.community.api.dto;

import com.marinbom2.finbroker.community.application.CommunityService.PostDetail;
import com.marinbom2.finbroker.community.domain.LoanType;
import com.marinbom2.finbroker.community.domain.Post;
import com.marinbom2.finbroker.community.domain.PostType;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.warning.domain.WarningType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 글 상세와 답변 (FR-CM-002). 주의 신호는 안내 문구(guidance)까지 담아 보낸다 */
@Schema(description = "글 상세")
public record PostDetailResponse(
        @Schema(description = "글 ID", example = "1") Long postId,
        @Schema(description = "글 유형. {code, description}") PostType postType,
        @Schema(description = "대출 유형. {code, description}") LoanType loanType,
        @Schema(description = "제목") String title,
        @Schema(description = "본문") String body,
        @Schema(description = "작성 시점 역할. {code, description}") RoleType authorRole,
        @Schema(description = "내가 쓴 글인지") boolean mine,
        @Schema(description = "가리키는 업체. 없으면 null") CompanyRef company,
        @Schema(description = "저장 때 찾은 주의 신호") List<WarningType> warnings,
        @Schema(description = "작성 시각 (UTC)") Instant createdAt,
        @Schema(description = "답변. 단 순서대로") List<AnswerResponse> answers
) {

    public static PostDetailResponse from(PostDetail detail, Long viewerId) {
        Post post = detail.post();
        return new PostDetailResponse(post.getId(), post.getPostType(), post.getLoanType(), post.getTitle(),
                post.getBody(), post.getAuthorRole(), post.isWrittenBy(viewerId), CompanyRef.from(detail.company()),
                post.getWarnings().stream().sorted().toList(), post.getCreatedAt(),
                detail.answers().stream().map(a -> AnswerResponse.from(a, post.getAuthorId(), viewerId)).toList());
    }
}
