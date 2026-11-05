package com.marinbom2.finbroker.company.application;

import com.marinbom2.finbroker.company.domain.RegistrationStatus;
import java.time.Duration;
import java.util.Map;

/** 운영자 현황: 상태별 업체 수 (모든 상태가 들어 있다, 없으면 0), 재확인 필요 수, 재확인 기간 */
public record OpsCompanySummary(Map<RegistrationStatus, Long> counts, long recheckDue, Duration recheckAfter) {
}
