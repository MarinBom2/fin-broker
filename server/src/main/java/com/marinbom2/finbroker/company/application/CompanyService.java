package com.marinbom2.finbroker.company.application;

import com.marinbom2.finbroker.company.domain.Company;
import com.marinbom2.finbroker.company.domain.CompanyRepository;
import com.marinbom2.finbroker.company.domain.CompanyType;
import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import com.marinbom2.finbroker.exception.BusinessException;
import com.marinbom2.finbroker.type.common.ErrorCode;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 업체 조회 (FR-CO-001), 운영자 등록·대조 (FR-CO-002) */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    /** 목록 순서는 돈과 무관한 기준만 쓴다: 상호 가나다순, 같으면 등록순 (md/domain-design.md §1) */
    private static final Sort LIST_ORDER = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final CompanyRepository companyRepository;
    private final Clock clock;

    /** @param page 1부터 */
    @Transactional(readOnly = true)
    public Page<Company> search(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, LIST_ORDER);
        if (!StringUtils.hasText(keyword)) {
            return companyRepository.findAll(pageable);
        }
        String trimmed = keyword.trim();
        return companyRepository.findByNameContainingOrRegistrationNumber(
                trimmed, Company.normalizeRegistrationNumber(trimmed), pageable);
    }

    @Transactional(readOnly = true)
    public Company get(Long companyId) {
        return findCompany(companyId);
    }

    @Transactional
    public Company register(String name, CompanyType companyType, String registrationNumber, String registrationAuthority) {
        String normalized = Company.normalizeRegistrationNumber(registrationNumber);
        if (companyRepository.existsByCompanyTypeAndRegistrationNumber(companyType, normalized)) {
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 등록된 업체입니다.");
        }
        try {
            Company company = companyRepository.saveAndFlush(
                    Company.register(name, companyType, normalized, registrationAuthority));
            log.info("[company] register companyId={} type={}", company.getId(), companyType);
            return company;
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 업체를 등록하면 위 확인을 둘 다 통과할 수 있다. 유니크 제약이 최종 판정
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 등록된 업체입니다.");
        }
    }

    /** 담당자 인증(CompanyMembership)이 생기면 EXPIRED 때 담당자 해제를 같은 트랜잭션에서 한다 (§4.1) */
    @Transactional
    public Company recordCheck(Long companyId, RegistrationStatus result, Long adminId) {
        Company company = findCompany(companyId);
        company.recordCheck(result, adminId, clock.instant());
        log.info("[company] check companyId={} result={} by={}", companyId, result, adminId);
        return company;
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_COMPANY));
    }
}
