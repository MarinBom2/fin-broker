package com.marinbom2.finbroker.community.api.dto;

import com.marinbom2.finbroker.community.domain.LoanType;
import com.marinbom2.finbroker.community.domain.Post;
import com.marinbom2.finbroker.community.domain.PostType;
import com.marinbom2.finbroker.company.domain.Company;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.warning.domain.WarningType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 글 목록의 한 줄. 작성자는 역할로만 보인다 (익명) */
@Schema(description = "글 목록 항목")
public record PostSummaryResponse(
        @Schema(description = "글 ID", example = "1") Long postId,
        @Schema(description = "글 유형. {code, description}") PostType postType,
        @Schema(description = "대출 유형. {code, description}") LoanType loanType,
        @Schema(description = "제목") String title,
        @Schema(description = "본문 앞부분 (최대 80자)") String excerpt,
        @Schema(description = "작성 시점 역할. {code, description}") RoleType authorRole,
        @Schema(description = "가리키는 업체. 없으면 null") CompanyRef company,
        @Schema(description = "저장 때 찾은 주의 신호") List<WarningType> warnings,
        @Schema(description = "답변 수", example = "2") int answerCount,
        @Schema(description = "작성 시각 (UTC)") Instant createdAt
) {

    static final int EXCERPT_LENGTH = 80;

    public static PostSummaryResponse from(Post post, Map<Long, Company> companies) {
        String body = post.getBody().replaceAll("\\s+", " ");
        String excerpt = body.length() > EXCERPT_LENGTH ? body.substring(0, EXCERPT_LENGTH) + "…" : body;
        // Map.of() 는 null 키 조회에 예외를 던진다. 업체가 없는 글은 조회하지 않는다
        Company company = post.getCompanyId() == null ? null : companies.get(post.getCompanyId());
        return new PostSummaryResponse(post.getId(), post.getPostType(), post.getLoanType(), post.getTitle(), excerpt,
                post.getAuthorRole(), CompanyRef.from(company),
                post.getWarnings().stream().sorted().toList(), post.getAnswerCount(), post.getCreatedAt());
    }
}
