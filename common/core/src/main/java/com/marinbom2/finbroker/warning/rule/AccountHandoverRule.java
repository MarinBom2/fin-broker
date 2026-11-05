package com.marinbom2.finbroker.warning.rule;

import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningRule;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 통장·체크카드·인증 수단을 넘기라는 요구 (WARN). 넘긴 사람도 처벌받을 수 있어 따로 경고한다 */
@Component
public class AccountHandoverRule implements WarningRule {

    private static final List<Pattern> PATTERNS = List.of(
            // 수단 + 넘기는 동작: "체크카드를 택배로 보내", "통장 비밀번호 알려"
            Pattern.compile("(?:통장|체크\\s*카드|보안\\s*카드|OTP|otp|공동\\s*인증서|공인\\s*인증서|인증서|카드\\s*비밀번호|통장\\s*비밀번호)"
                    + "[^.!?\\n]{0,15}(?:보내|넘기|넘겨|맡기|맡겨|전달|빌려|대여|양도|택배|퀵|알려)"),
            // 거래 자체를 말하는 표현
            Pattern.compile("대포\\s*통장|통장\\s*(?:대여|매입|임대)|카드\\s*(?:대여|매입)"));

    @Override
    public List<Warning> inspect(WarningTarget target) {
        return PatternRules.findAll(target.text(), WarningType.ACCOUNT_HANDOVER, PATTERNS);
    }
}
