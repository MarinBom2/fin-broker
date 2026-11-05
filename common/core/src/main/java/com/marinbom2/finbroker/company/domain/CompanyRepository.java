package com.marinbom2.finbroker.company.domain;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    /** 업체 조회: 상호 일부 또는 등록번호 전체로 찾는다. 정렬은 호출부가 돈과 무관한 기준(상호순)으로 준다 */
    Page<Company> findByNameContainingOrRegistrationNumber(String name, String registrationNumber, Pageable pageable);

    /**
     * 운영자 목록: 상태·검색어·마지막 대조 시각으로 거른다 (null 이면 그 조건 없음). 상호 일부 또는 등록번호 전체.
     * checkedBefore 는 "재확인 필요" 걸러 보기용 (이 시각 이전에 대조한 업체). 정렬은 호출부가 준다.
     */
    @Query("""
            select c from Company c
            where (:status is null or c.registrationStatus = :status)
              and (:keyword is null or c.name like concat('%', :keyword, '%') or c.registrationNumber = :number)
              and (:checkedBefore is null or c.lastCheckedAt <= :checkedBefore)
            """)
    Page<Company> searchOps(@Param("status") RegistrationStatus status, @Param("keyword") String keyword,
                            @Param("number") String number, @Param("checkedBefore") Instant checkedBefore,
                            Pageable pageable);

    /** 운영자 현황: 상태별 업체 수 */
    long countByRegistrationStatus(RegistrationStatus status);

    /** 운영자 현황: 이 시각 이전에 대조한 해당 상태 업체 수 (재확인 필요) */
    long countByRegistrationStatusAndLastCheckedAtLessThanEqual(RegistrationStatus status, Instant checkedBefore);

    /** 등록 중복 확인 */
    boolean existsByCompanyTypeAndRegistrationNumber(CompanyType companyType, String registrationNumber);
}
