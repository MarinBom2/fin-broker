package com.marinbom2.finbroker.warning;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 피해 예방 규칙 설정 ({@code warning.*}).
 *
 * @param interestCapPercent 법정 최고금리 (연 %). 이 값을 넘는 금리 표기를 경고한다
 */
@ConfigurationProperties("warning")
public record WarningProperties(@DefaultValue("20") BigDecimal interestCapPercent) {
}
