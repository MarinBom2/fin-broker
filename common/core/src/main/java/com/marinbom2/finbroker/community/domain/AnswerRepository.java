package com.marinbom2.finbroker.community.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /** 글 상세: 게시 상태 답변을 단 순서대로 */
    List<Answer> findByPostIdAndStatusOrderByIdAsc(Long postId, PostStatus status);
}
