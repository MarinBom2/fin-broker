package com.marinbom2.finbroker.moderation.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /** 같은 회원이 같은 대상을 이미 신고했는지 (최종 판정은 유니크 제약) */
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTarget targetType, Long targetId);

    /** 자동 숨김 판단: 대상의 접수 신고 수 (한 회원 한 건이라 곧 신고한 회원 수) */
    long countByTargetTypeAndTargetIdAndStatus(ReportTarget targetType, Long targetId, ReportStatus status);

    /** 대상 단위 처리: 같은 대상의 접수 신고 전부 */
    List<Report> findByTargetTypeAndTargetIdAndStatus(ReportTarget targetType, Long targetId, ReportStatus status);

    /**
     * 운영자 목록: 대상마다 가장 최근 신고 ID 하나 (대상 단위로 처리하므로 목록도 대상 단위). 최근 신고가 위.
     * 정렬은 쿼리에 있으므로 Pageable 에 정렬을 넣지 않는다.
     */
    @Query(value = """
            select max(r.id) from Report r
            where r.status = :status
            group by r.targetType, r.targetId
            order by max(r.id) desc
            """,
            countQuery = """
            select count(r) from Report r
            where r.status = :status
              and r.id in (select max(r2.id) from Report r2 where r2.status = :status group by r2.targetType, r2.targetId)
            """)
    Page<Long> findLatestIdPerTarget(@Param("status") ReportStatus status, Pageable pageable);

    /** 내가 신고했는지 표시용 */
    List<Report> findByReporterIdAndPostId(Long reporterId, Long postId);
}
