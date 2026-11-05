package com.marinbom2.finbroker.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.finbroker.community.domain.exception.NotAuthorException;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 글·답변 수정·삭제·숨김 (FR-CM-001·003, FR-MD-002) */
class PostEditDeleteTest {

    private static final long AUTHOR = 1L;
    private static final long OTHER = 2L;

    @Test
    @DisplayName("작성자는 글을 고칠 수 있고, 주의 신호는 고친 내용의 검사 결과로 바뀐다")
    void editPost() {
        Post post = post(Set.of(WarningType.UPFRONT_PAYMENT));

        post.edit(AUTHOR, PostType.REVIEW, LoanType.SECURED, 5L, " 새 제목 ", " 새 본문 ", Set.of(WarningType.ACCOUNT_HANDOVER));

        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getBody()).isEqualTo("새 본문");
        assertThat(post.getPostType()).isEqualTo(PostType.REVIEW);
        assertThat(post.getCompanyId()).isEqualTo(5L);
        assertThat(post.getWarnings()).containsExactly(WarningType.ACCOUNT_HANDOVER);
    }

    @Test
    @DisplayName("작성자가 아니면 고치거나 지울 수 없다 (NotAuthorException), 내용은 그대로")
    void otherMemberCannotEditOrDelete() {
        Post post = post(Set.of());

        assertThatThrownBy(() -> post.edit(OTHER, PostType.INFO, LoanType.OTHER, null, "x", "y", Set.of()))
                .isInstanceOf(NotAuthorException.class);
        assertThatThrownBy(() -> post.delete(OTHER)).isInstanceOf(NotAuthorException.class);
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.isPublished()).isTrue();
    }

    @Test
    @DisplayName("글을 지우면 상태는 DELETED, 제목·본문·업체·신호를 비운다")
    void deletePost() {
        Post post = Post.write(AUTHOR, RoleType.USER, PostType.REVIEW, LoanType.CREDIT, 3L, "제목", "본문",
                Set.of(WarningType.UPFRONT_PAYMENT));

        post.delete(AUTHOR);

        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(post.getTitle()).isEmpty();
        assertThat(post.getBody()).isEmpty();
        assertThat(post.getCompanyId()).isNull();
        assertThat(post.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("숨김·복구는 상태가 맞을 때만 바뀐다. 지운 글은 복구되지 않는다")
    void hideAndRestorePost() {
        Post post = post(Set.of());

        post.hide();
        assertThat(post.getStatus()).isEqualTo(PostStatus.HIDDEN);
        post.restore();
        assertThat(post.isPublished()).isTrue();

        post.delete(AUTHOR);
        post.restore();
        post.hide();
        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
    }

    @Test
    @DisplayName("답변: 고치기·지우기는 작성자만, 지우거나 숨기면 글의 답변 수가 내려가고 복구하면 다시 오른다")
    void answerLifecycle() {
        Post post = post(Set.of());
        Answer mine = answer(post, 10L, OTHER);
        Answer hidden = answer(post, 11L, AUTHOR);
        assertThat(post.getAnswerCount()).isEqualTo(2);

        assertThatThrownBy(() -> mine.edit(AUTHOR, "x", Set.of())).isInstanceOf(NotAuthorException.class);
        mine.edit(OTHER, " 고친 답변 ", Set.of(WarningType.UPFRONT_PAYMENT));
        assertThat(mine.getBody()).isEqualTo("고친 답변");
        assertThat(mine.getWarnings()).containsExactly(WarningType.UPFRONT_PAYMENT);

        mine.delete(OTHER, post);
        assertThat(mine.getStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(mine.getBody()).isEmpty();
        assertThat(post.getAnswerCount()).isEqualTo(1);

        hidden.hide(post);
        hidden.hide(post);
        assertThat(post.getAnswerCount()).isZero();
        hidden.restore(post);
        assertThat(post.getAnswerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 글로 답변 수를 바꾸려 하면 거부한다")
    void answerCountOnlyOnItsPost() {
        Post post = post(Set.of());
        Post another = post(Set.of());
        ReflectionTestUtils.setField(another, "id", 999L);
        Answer answer = answer(post, 10L, OTHER);

        assertThatThrownBy(() -> answer.hide(another)).isInstanceOf(IllegalArgumentException.class);
        assertThat(another.getAnswerCount()).isZero();
    }

    private static Post post(Set<WarningType> warnings) {
        Post post = Post.write(AUTHOR, RoleType.USER, PostType.QUESTION, LoanType.CREDIT, null, "제목", "본문", warnings);
        ReflectionTestUtils.setField(post, "id", 100L);
        return post;
    }

    private static Answer answer(Post post, long id, long authorId) {
        Answer answer = Answer.write(post, authorId, RoleType.USER, "답변", Set.of());
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }
}
