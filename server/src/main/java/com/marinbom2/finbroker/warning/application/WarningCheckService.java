package com.marinbom2.finbroker.warning.application;

import com.marinbom2.finbroker.company.domain.CompanyRepository;
import com.marinbom2.finbroker.exception.BusinessException;
import com.marinbom2.finbroker.type.common.ErrorCode;
import com.marinbom2.finbroker.warning.domain.WarningInspector;
import com.marinbom2.finbroker.warning.domain.WarningReport;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피해 예방 검사 (FR-WN-001). 아무것도 저장하지 않는다. 검사한 문장도 로그에 남기지 않는다 (개인정보).
 * 글 저장(FR-WN-002)은 커뮤니티 서비스가 같은 WarningInspector 를 다시 돌린다.
 */
@Service
@RequiredArgsConstructor
public class WarningCheckService {

    private final WarningInspector warningInspector;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public WarningReport check(String title, String body, Long companyId) {
        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_COMPANY);
        }
        return warningInspector.inspect(WarningTarget.of(title, body, companyId));
    }
}
