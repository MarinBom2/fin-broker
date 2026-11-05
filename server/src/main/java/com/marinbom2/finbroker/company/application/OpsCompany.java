package com.marinbom2.finbroker.company.application;

import com.marinbom2.finbroker.company.domain.Company;

/** 운영자 화면의 업체 한 건. 재확인 필요 여부는 설정 기간과 지금 시각으로 서비스가 정한다 */
public record OpsCompany(Company company, boolean recheckDue) {
}
