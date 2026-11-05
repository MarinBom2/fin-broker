package com.marinbom2.finbroker.community.domain;

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
import jakarta.persistence.Version;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커뮤니티 글 (FR-CM-001·002, md/domain-design.md §5.4). 작성자는 화면에 역할로만 보인다 (익명).
 * 저장 전 피해 예방 검사를 통과해야 하고, 주의(WARN) 신호는 글에 남겨 배지로 보여 준다 (§4.2).
 */
@Getter
@Entity
@Table(name = "post", indexes = {
        @Index(name = "idx_post_status_type", columnList = "status, post_type, id"),
        @Index(name = "idx_post_company", columnList = "company_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 작성 시점 역할. 나중에 역할이 바뀌어도 당시 "등록 업체 작성"이었다는 사실은 남긴다 */
    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 20)
    private RoleType authorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    /** 후기·피해 사례가 가리키는 업체. 없으면 null */
    @Column(name = "company_id")
    private Long companyId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    /** 목록 표시용. 답변 추가와 같은 트랜잭션에서 바꾼다 */
    @Column(name = "answer_count", nullable = false)
    private int answerCount;

    /** 저장 시 찾은 주의 신호 (WARN 만. BLOCK 이 있으면 저장되지 않는다) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_warning", joinColumns = @JoinColumn(name = "post_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false, length = 30)
    private Set<WarningType> warnings = EnumSet.noneOf(WarningType.class);

    @Version
    private Long version;

    public static Post write(Long authorId, RoleType authorRole, PostType postType, LoanType loanType, Long companyId,
                             String title, String body, Set<WarningType> warnings) {
        Post post = new Post();
        post.authorId = authorId;
        post.authorRole = authorRole;
        post.postType = postType;
        post.loanType = loanType;
        post.companyId = companyId;
        post.title = title.strip();
        post.body = body.strip();
        post.status = PostStatus.PUBLISHED;
        post.answerCount = 0;
        post.warnings = warnings.isEmpty() ? EnumSet.noneOf(WarningType.class) : EnumSet.copyOf(warnings);
        return post;
    }

    /** 답변이 하나 늘었다. 답변 저장과 같은 트랜잭션에서 부른다 (@Version 으로 동시 갱신을 막는다) */
    public void increaseAnswerCount() {
        this.answerCount++;
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    public boolean isWrittenBy(Long memberId) {
        return authorId.equals(memberId);
    }
}
