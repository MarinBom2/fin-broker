package com.marinbom2.finbroker.company.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    /** 업체 조회: 상호 일부 또는 등록번호 전체로 찾는다. 정렬은 호출부가 돈과 무관한 기준(상호순)으로 준다 */
    Page<Company> findByNameContainingOrRegistrationNumber(String name, String registrationNumber, Pageable pageable);

    /** 등록 중복 확인 */
    boolean existsByCompanyTypeAndRegistrationNumber(CompanyType companyType, String registrationNumber);
}
