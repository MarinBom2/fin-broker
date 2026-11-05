package com.marinbom2.finbroker.warning.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.marinbom2.finbroker.warning.WarningProperties;
import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InterestCapRuleTest {

    private final InterestCapRule rule = new InterestCapRule(new WarningProperties(new BigDecimal("20")));

    @ParameterizedTest(name = "경고: {0}")
    @ValueSource(strings = {
            "연 30% 로 해 준대요",
            "금리 25%라고 하네요",
            "연이율 24.5%",
            "월 2% 이자라는데 괜찮나요",
            "월이자 3프로",
            "매일 1% 씩 이자가 붙어요",
            "주 1퍼센트",
    })
    void overCap(String text) {
        List<Warning> warnings = inspect(text);

        assertThat(warnings).hasSize(1);
        assertThat(warnings.getFirst().type()).isEqualTo(WarningType.OVER_INTEREST_CAP);
    }

    @ParameterizedTest(name = "통과: {0}")
    @ValueSource(strings = {
            "연 20% 넘으면 불법이래요",
            "금리 15%면 괜찮은 편인가요",
            "월 1.5% 이자",
            "30일 5% 할인",
            "1년 5% 적금",
            "월세 3% 인상",
            "수수료 30%",
            "주식 30% 수익",
    })
    void notOverCap(String text) {
        assertThat(inspect(text)).isEmpty();
    }

    @Test
    @DisplayName("월·일 금리는 연 금리로 환산한 값을 근거에 붙인다")
    void convertedEvidence() {
        assertThat(inspect("월 3% 라고 했어요").getFirst().evidence()).isEqualTo("월 3% (연 36%로 환산)");
        assertThat(inspect("연 30%").getFirst().evidence()).isEqualTo("연 30%");
    }

    @Test
    @DisplayName("연체 이자도 최고금리 안이어야 해서 경고한다 `[확인]`")
    void lateInterest() {
        assertThat(inspect("연체 이자 30%")).hasSize(1);
    }

    @Test
    @DisplayName("'연 금리 30%' 는 기간 표현과 금리 표현에 둘 다 걸려도 한 번만 경고한다")
    void noDoubleCount() {
        assertThat(inspect("연 금리 30%")).hasSize(1);
    }

    @Test
    @DisplayName("기준값은 설정을 따른다")
    void configurableCap() {
        InterestCapRule lowerCap = new InterestCapRule(new WarningProperties(new BigDecimal("15")));

        assertThat(lowerCap.inspect(new WarningTarget("연 18%", null))).hasSize(1);
    }

    private List<Warning> inspect(String text) {
        return rule.inspect(new WarningTarget(text, null));
    }
}
