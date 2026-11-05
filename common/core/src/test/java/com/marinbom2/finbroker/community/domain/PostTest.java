package com.marinbom2.finbroker.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningReport;
import com.marinbom2.finbroker.warning.domain.WarningType;
import com.marinbom2.finbroker.warning.domain.exception.ContentBlockedException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PostTest {

    @Test
    @DisplayName("글쓰기: 게시 상태, 답변 0, 앞뒤 공백 제거, 작성 시점 역할과 주의 신호를 남긴다 (FR-CM-001)")
    void write() {
        Post post = Post.write(1L, RoleType.USER, PostType.QUESTION, LoanType.CREDIT, null,
                "  제목 ", " 본문 ", Set.of(WarningType.UPFRONT_PAYMENT));

        assertThat(post.isPublished()).isTrue();
        assertThat(post.getAnswerCount()).isZero();
        assertThat(post.getTitle()).isEqualTo("제목");
        assertThat(post.getBody()).isEqualTo("본문");
        assertThat(post.getAuthorRole()).isEqualTo(RoleType.USER);
        assertThat(post.getWarnings()).containsExactly(WarningType.UPFRONT_PAYMENT);
        assertThat(post.isWrittenBy(1L)).isTrue();
        assertThat(post.isWrittenBy(2L)).isFalse();
    }

    @Test
    @DisplayName("답변을 달면 글의 답변 수가 같이 오른다. 게시 상태가 아닌 글에는 달 수 없다 (FR-CM-003)")
    void answer() {
        Post post = Post.write(1L, RoleType.USER, PostType.QUESTION, LoanType.CREDIT, null, "제목", "본문", Set.of());
        ReflectionTestUtils.setField(post, "id", 10L);

        Answer answer = Answer.write(post, 2L, RoleType.COMPANY, " 답변 ", Set.of());

        assertThat(answer.getPostId()).isEqualTo(10L);
        assertThat(answer.getBody()).isEqualTo("답변");
        assertThat(post.getAnswerCount()).isEqualTo(1);

        ReflectionTestUtils.setField(post, "status", PostStatus.HIDDEN);
        assertThatThrownBy(() -> Answer.write(post, 2L, RoleType.USER, "x", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(post.getAnswerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("검사 결과: BLOCK 이 있으면 ContentBlockedException, 저장에 남길 신호는 WARN 만")
    void reportForSaving() {
        WarningReport blocked = new WarningReport(List.of(
                new Warning(WarningType.CONTACT_EXPOSURE, "***-****-5678"),
                new Warning(WarningType.UPFRONT_PAYMENT, "선입금")));
        WarningReport warnOnly = new WarningReport(List.of(new Warning(WarningType.UPFRONT_PAYMENT, "선입금")));

        assertThatThrownBy(blocked::requireNotBlocked)
                .isInstanceOf(ContentBlockedException.class)
                .satisfies(e -> assertThat(((ContentBlockedException) e).getWarnings()).hasSize(2));
        warnOnly.requireNotBlocked();
        assertThat(warnOnly.warnTypes()).containsExactly(WarningType.UPFRONT_PAYMENT);
        assertThat(blocked.warnTypes()).containsExactly(WarningType.UPFRONT_PAYMENT);
    }
}
