package com.marinbom2.finbroker.community.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /** 글 상세: 게시 상태 답변을 단 순서대로 */
    List<Answer> findByPostIdAndStatusOrderByIdAsc(Long postId, PostStatus status);

    /** 답변 수정·삭제·신고: 그 글의 게시 상태 답변 */
    Optional<Answer> findByIdAndPostIdAndStatus(Long id, Long postId, PostStatus status);
}
