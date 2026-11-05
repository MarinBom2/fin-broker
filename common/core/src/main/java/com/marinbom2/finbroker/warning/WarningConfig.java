package com.marinbom2.finbroker.warning;

import com.marinbom2.finbroker.warning.domain.WarningInspector;
import com.marinbom2.finbroker.warning.domain.WarningRule;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 규칙 빈(@Component) 전부를 모아 검사기 하나로 묶는다. 규칙을 더해도 여기는 바뀌지 않는다 */
@Configuration
@EnableConfigurationProperties(WarningProperties.class)
public class WarningConfig {

    @Bean
    public WarningInspector warningInspector(List<WarningRule> rules) {
        return new WarningInspector(rules);
    }
}
