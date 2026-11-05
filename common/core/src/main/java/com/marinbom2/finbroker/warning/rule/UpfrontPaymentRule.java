package com.marinbom2.finbroker.warning.rule;

import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningRule;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 대출 전에 돈을 먼저 보내라는 요구 (WARN). 대출 사기에서 자주 보이는 표현을 찾는다 */
@Component
public class UpfrontPaymentRule implements WarningRule {

    private static final List<Pattern> PATTERNS = List.of(
            // 선입금을 직접 말하는 표현
            Pattern.compile("선입금|선납금|먼저\\s*입금|미리\\s*입금|입금\\s*먼저|선\\s*수수료"),
            // 명목을 붙여 송금을 요구하는 표현: "보증금 30만원 입금하시면", "작업비를 보내 주셔야"
            Pattern.compile("(?:수수료|보증금|작업비|전산\\s*작업비|공증비|보증\\s*보험료|인지대|등록비|예치금)"
                    + "[^.!?\\n]{0,15}(?:입금|송금|이체|보내|내셔야|내야|요구)"),
            // 신용등급·한도를 "작업"해 준다는 표현
            Pattern.compile("(?:신용|등급|한도)\\s*작업|작업\\s*대출"));

    @Override
    public List<Warning> inspect(WarningTarget target) {
        return PatternRules.findAll(target.text(), WarningType.UPFRONT_PAYMENT, PATTERNS);
    }
}
