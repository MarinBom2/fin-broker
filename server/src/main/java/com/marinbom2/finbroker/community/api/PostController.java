package com.marinbom2.finbroker.community.api;

import com.marinbom2.finbroker.common.api.PageResponse;
import com.marinbom2.finbroker.community.api.dto.AnswerResponse;
import com.marinbom2.finbroker.community.api.dto.AnswerWriteRequest;
import com.marinbom2.finbroker.community.api.dto.PostDetailResponse;
import com.marinbom2.finbroker.community.api.dto.PostSummaryResponse;
import com.marinbom2.finbroker.community.api.dto.PostWriteRequest;
import com.marinbom2.finbroker.community.application.CommunityService;
import com.marinbom2.finbroker.community.application.CommunityService.AnswerResult;
import com.marinbom2.finbroker.community.application.CommunityService.PostDetail;
import com.marinbom2.finbroker.community.application.CommunityService.PostPage;
import com.marinbom2.finbroker.community.domain.LoanType;
import com.marinbom2.finbroker.community.domain.PostType;
import com.marinbom2.finbroker.member.domain.Member;
import com.marinbom2.finbroker.moderation.application.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** SCR-02~04: 커뮤니티 글·답변. 작성자는 역할로만 보인다 (익명). 고치기·지우기는 작성자만 (403) */
@Tag(name = "커뮤니티", description = "글 목록·상세·글쓰기·수정·삭제·답변 (SCR-02~04)")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final CommunityService communityService;
    private final ReportService reportService;

    @Operation(summary = "글 목록", description = "FR-CM-002. 게시 상태 글을 최신순으로. 글 유형·대출 유형으로 거른다")
    @GetMapping
    public PageResponse<PostSummaryResponse> list(
            @Parameter(description = "글 유형 코드. 없으면 전체") @RequestParam(required = false) PostType postType,
            @Parameter(description = "대출 유형 코드. 없으면 전체") @RequestParam(required = false) LoanType loanType,
            @Parameter(description = "페이지 번호 (1부터)") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "페이지 번호는 1부터입니다.") int page,
            @Parameter(description = "페이지 크기 (최대 50)") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "페이지 크기는 1 이상입니다.") @Max(value = 50, message = "페이지 크기는 50 이하입니다.") int size) {
        PostPage result = communityService.list(postType, loanType, page, size);
        return PageResponse.of(result.posts(), post -> PostSummaryResponse.from(post, result.companies()));
    }

    @Operation(summary = "글쓰기",
            description = "FR-CM-001, FR-WN-002. 저장 전에 피해 예방 검사를 다시 돈다. 연락처·개인정보가 있으면 400 CONTENT_BLOCKED(warnings 포함), "
                    + "주의 신호는 글에 남겨 배지로 보여 준다")
    @ApiResponse(responseCode = "400", description = "CONTENT_BLOCKED, INVALID_REQUEST_DATA",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_COMPANY",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse write(@AuthenticationPrincipal Member member, @Valid @RequestBody PostWriteRequest request) {
        PostDetail written = communityService.write(member, request.postType(), request.loanType(), request.companyId(),
                request.title(), request.body());
        return PostDetailResponse.from(written, member.getId(), Set.of());
    }

    @Operation(summary = "글 상세", description = "FR-CM-002. 본문, 가리키는 업체, 주의 신호, 답변, 내가 신고했는지")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_POST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{postId}")
    public PostDetailResponse get(@AuthenticationPrincipal Member member, @PathVariable Long postId) {
        PostDetail detail = communityService.get(postId);
        return PostDetailResponse.from(detail, member.getId(), reportService.reportedBy(member.getId(), postId));
    }

    @Operation(summary = "글 고치기",
            description = "FR-CM-001, FR-WN-002. 작성자만. 고친 내용으로 피해 예방 검사를 다시 돈다")
    @ApiResponse(responseCode = "400", description = "CONTENT_BLOCKED, INVALID_REQUEST_DATA",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_POST, NOT_FOUND_COMPANY",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{postId}")
    public PostDetailResponse edit(@AuthenticationPrincipal Member member, @PathVariable Long postId,
                                   @Valid @RequestBody PostWriteRequest request) {
        PostDetail edited = communityService.edit(member, postId, request.postType(), request.loanType(),
                request.companyId(), request.title(), request.body());
        return PostDetailResponse.from(edited, member.getId(), reportService.reportedBy(member.getId(), postId));
    }

    @Operation(summary = "글 지우기", description = "FR-CM-001. 작성자만. 내용을 비우고 목록·상세에서 사라진다")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_POST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Member member, @PathVariable Long postId) {
        communityService.delete(member, postId);
    }

    @Operation(summary = "답변 달기",
            description = "FR-CM-003, FR-WN-002. 연락처가 있으면 400 CONTENT_BLOCKED. 글의 답변 수가 같이 오른다")
    @ApiResponse(responseCode = "400", description = "CONTENT_BLOCKED, INVALID_REQUEST_DATA",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_POST",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{postId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public AnswerResponse answer(@AuthenticationPrincipal Member member, @PathVariable Long postId,
                                 @Valid @RequestBody AnswerWriteRequest request) {
        AnswerResult result = communityService.answer(member, postId, request.body());
        return AnswerResponse.from(result.answer(), result.postAuthorId(), member.getId(), false);
    }

    @Operation(summary = "답변 고치기", description = "FR-CM-003, FR-WN-002. 작성자만. 피해 예방 검사를 다시 돈다")
    @ApiResponse(responseCode = "400", description = "CONTENT_BLOCKED, INVALID_REQUEST_DATA",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_POST, NOT_FOUND_ANSWER",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{postId}/answers/{answerId}")
    public AnswerResponse editAnswer(@AuthenticationPrincipal Member member, @PathVariable Long postId,
                                     @PathVariable Long answerId, @Valid @RequestBody AnswerWriteRequest request) {
        AnswerResult result = communityService.editAnswer(member, postId, answerId, request.body());
        return AnswerResponse.from(result.answer(), result.postAuthorId(), member.getId(), false);
    }

    @Operation(summary = "답변 지우기", description = "FR-CM-003. 작성자만. 글의 답변 수가 같이 내려간다")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND_POST, NOT_FOUND_ANSWER",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{postId}/answers/{answerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnswer(@AuthenticationPrincipal Member member, @PathVariable Long postId,
                             @PathVariable Long answerId) {
        communityService.deleteAnswer(member, postId, answerId);
    }
}
