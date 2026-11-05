package com.marinbom2.finbroker.community.domain;

import com.marinbom2.finbroker.community.domain.exception.NotAuthorException;
import com.marinbom2.finbroker.domain.base.BaseEntity;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.warning.domain.WarningType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 글에 단 답변 (FR-CM-003, md/domain-design.md §5.5). 작성자는 역할로만 보인다 */
@Getter
@Entity
@Table(name = "answer", indexes = @Index(name = "idx_answer_post", columnList = "post_id, status, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 20)
    private RoleType authorRole;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "answer_warning", joinColumns = @JoinColumn(name = "answer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 30)
    private Set<WarningType> warnings = EnumSet.noneOf(WarningType.class);

    /**
     * 답변을 단다. 글의 답변 수도 같이 올린다 (둘을 따로 바꾸는 코드를 만들지 않는다, md/solid.md S).
     *
     * @param post 게시 상태인 글. 호출부(서비스)가 게시 상태로 조회해 넘긴다
     */
    public static Answer write(Post post, Long authorId, RoleType authorRole, String body, Set<WarningType> warnings) {
        if (!post.isPublished()) {
            throw new IllegalArgumentException("게시 상태가 아닌 글에는 답변할 수 없습니다: " + post.getId());
        }
        Answer answer = new Answer();
        answer.postId = post.getId();
        answer.authorId = authorId;
        answer.authorRole = authorRole;
        answer.body = body.strip();
        answer.status = PostStatus.PUBLISHED;
        answer.warnings = warnings.isEmpty() ? EnumSet.noneOf(WarningType.class) : EnumSet.copyOf(warnings);
        post.increaseAnswerCount();
        return answer;
    }

    /**
     * 작성자가 답변을 고친다 (FR-CM-003). 주의 신호는 다시 검사한 결과로 바꾼다.
     *
     * @throws NotAuthorException 작성자가 아닐 때
     */
    public void edit(Long memberId, String body, Set<WarningType> warnings) {
        requireAuthor(memberId);
        requirePublished();
        this.body = body.strip();
        this.warnings.clear();
        this.warnings.addAll(warnings);
    }

    /**
     * 작성자가 답변을 지운다. 내용을 비우고, 글의 답변 수도 같이 내린다.
     *
     * @throws NotAuthorException 작성자가 아닐 때
     */
    public void delete(Long memberId, Post post) {
        requireAuthor(memberId);
        requirePublished();
        requireSamePost(post);
        this.status = PostStatus.DELETED;
        this.body = "";
        this.warnings.clear();
        post.decreaseAnswerCount();
    }

    /** 운영자 숨김·신고 누적. 게시 상태일 때만 바뀌고, 글의 답변 수도 같이 내린다 */
    public void hide(Post post) {
        requireSamePost(post);
        if (status == PostStatus.PUBLISHED) {
            this.status = PostStatus.HIDDEN;
            post.decreaseAnswerCount();
        }
    }

    /** 숨김을 푼다. 숨김 상태일 때만 바뀌고, 글의 답변 수도 같이 올린다 */
    public void restore(Post post) {
        requireSamePost(post);
        if (status == PostStatus.HIDDEN) {
            this.status = PostStatus.PUBLISHED;
            post.increaseAnswerCount();
        }
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    public boolean isWrittenBy(Long memberId) {
        return authorId.equals(memberId);
    }

    private void requireAuthor(Long memberId) {
        if (!isWrittenBy(memberId)) {
            throw new NotAuthorException("answer", id, memberId);
        }
    }

    private void requirePublished() {
        if (!isPublished()) {
            throw new IllegalStateException("게시 상태가 아닌 답변: " + id);
        }
    }

    /** 답변 수는 이 답변이 달린 글에만 반영해야 한다 */
    private void requireSamePost(Post post) {
        if (!postId.equals(post.getId())) {
            throw new IllegalArgumentException("답변 " + id + " 은 글 " + post.getId() + " 의 답변이 아닙니다");
        }
    }
}
