package com.marinbom2.finbroker.company.domain.exception;

import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import lombok.Getter;

/** 허용되지 않는 등록 상태 전이. API 에서는 409 COMPANY_STATE_CONFLICT 로 바꾼다 (GlobalExceptionHandler) */
@Getter
public class IllegalCompanyStateException extends RuntimeException {

    private final Long companyId;
    private final RegistrationStatus from;
    private final RegistrationStatus to;

    public IllegalCompanyStateException(Long companyId, RegistrationStatus from, RegistrationStatus to) {
        super("company " + companyId + ": " + from + " -> " + to);
        this.companyId = companyId;
        this.from = from;
        this.to = to;
    }
}
