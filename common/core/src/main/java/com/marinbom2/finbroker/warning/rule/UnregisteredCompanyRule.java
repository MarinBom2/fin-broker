package com.marinbom2.finbroker.warning.rule;

import com.marinbom2.finbroker.company.domain.CompanyRepository;
import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningRule;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 글이 가리키는 업체가 등록 확인(VERIFIED) 상태가 아님 (WARN).
 * 없는 업체 ID 는 판정하지 않는다. 존재 여부 검증은 호출부(서비스)의 몫이다.
 */
@Component
@RequiredArgsConstructor
public class UnregisteredCompanyRule implements WarningRule {

    private final CompanyRepository companyRepository;

    @Override
    public List<Warning> inspect(WarningTarget target) {
        if (target.companyId() == null) {
            return List.of();
        }
        return companyRepository.findById(target.companyId())
                .filter(company -> !company.isVerified())
                .map(company -> List.of(new Warning(WarningType.UNREGISTERED_COMPANY,
                        company.getName() + " (" + company.getRegistrationStatus().getDescription() + ")")))
                .orElse(List.of());
    }
}
