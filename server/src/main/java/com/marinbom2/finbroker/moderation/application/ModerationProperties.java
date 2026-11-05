package com.marinbom2.finbroker.moderation.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 신고 처리 설정 ({@code moderation.*}).
 *
 * @param autoHideReportCount 서로 다른 회원의 접수 신고가 이 수에 이르면 운영자 확인 전에 먼저 숨긴다 (md/domain-design.md §4.4)
 */
@ConfigurationProperties("moderation")
public record ModerationProperties(@DefaultValue("3") int autoHideReportCount) {
}
