package com.marinbom2.finbroker.warning.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.marinbom2.finbroker.warning.domain.WarningTarget;
import com.marinbom2.finbroker.warning.domain.WarningType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 선입금 요구·통장 전달 요구 규칙 */
class FraudPatternRulesTest {

    private final UpfrontPaymentRule upfront = new UpfrontPaymentRule();
    private final AccountHandoverRule handover = new AccountHandoverRule();

    @ParameterizedTest(name = "선입금 경고: {0}")
    @ValueSource(strings = {
            "선입금 30만원 하면 바로 승인된대요",
            "보증금 50만원 먼저 입금하라고 해요",
            "작업비를 보내 주셔야 진행됩니다",
            "신용작업 해서 등급 올려 준대요",
            "전산 작업비 송금 요구",
    })
    void upfrontPayment(String text) {
        assertThat(upfront.inspect(target(text)))
                .isNotEmpty()
                .allMatch(w -> w.type() == WarningType.UPFRONT_PAYMENT);
    }

    @ParameterizedTest(name = "선입금 통과: {0}")
    @ValueSource(strings = {
            "중도상환 수수료가 있나요",
            "대출 받고 매달 입금하면 되나요",
            "보증금 대출 문의드려요",
    })
    void noUpfrontPayment(String text) {
        assertThat(upfront.inspect(target(text))).isEmpty();
    }

    @ParameterizedTest(name = "통장 전달 경고: {0}")
    @ValueSource(strings = {
            "체크카드를 택배로 보내 달래요",
            "통장 비밀번호 알려 달라고",
            "OTP 를 맡기면 한도가 올라간다고",
            "통장 대여 하면 수익",
            "대포통장 아닌가요",
    })
    void accountHandover(String text) {
        assertThat(handover.inspect(target(text)))
                .isNotEmpty()
                .allMatch(w -> w.type() == WarningType.ACCOUNT_HANDOVER);
    }

    @ParameterizedTest(name = "통장 전달 통과: {0}")
    @ValueSource(strings = {
            "통장 사본만 제출했어요",
            "체크카드로 상환해도 되나요",
    })
    void noAccountHandover(String text) {
        assertThat(handover.inspect(target(text))).isEmpty();
    }

    private static WarningTarget target(String text) {
        return new WarningTarget(text, null);
    }
}
