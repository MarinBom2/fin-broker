package com.marinbom2.finbroker.community.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 목록: 게시 상태만, 유형 필터는 null 이면 전체. 정렬은 호출부가 준다 */
    @Query("""
            select p from Post p
            where p.status = :status
              and (:postType is null or p.postType = :postType)
              and (:loanType is null or p.loanType = :loanType)
            """)
    Page<Post> search(@Param("status") PostStatus status, @Param("postType") PostType postType,
                      @Param("loanType") LoanType loanType, Pageable pageable);

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);
}
