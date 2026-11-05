package com.marinbom2.finbroker.company.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 업체 설정 ({@code company.*}).
 *
 * @param recheckAfter 등록 확인한 지 이 기간이 지나면 운영자 화면에 "재확인 필요"로 보인다 (md/domain-design.md §4.1)
 */
@ConfigurationProperties("company")
public record CompanyProperties(@DefaultValue("90d") Duration recheckAfter) {
}
