package com.marinbom2.finbroker.warning.rule;

import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 정규식 규칙들이 같이 쓰는 도구. 규칙 클래스 밖으로 공개하지 않는다 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PatternRules {

    /** 패턴마다 찾은 문장 조각을 그대로 근거로 쓴다 */
    static List<Warning> findAll(String text, WarningType type, List<Pattern> patterns) {
        List<Warning> warnings = new ArrayList<>();
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                warnings.add(new Warning(type, matcher.group()));
            }
        }
        return warnings;
    }

    /** 끝 네 자리만 남기고 숫자를 가린다 (010-1234-5678 -> ***-****-5678). 구분 기호는 그대로 둔다 */
    static String maskDigits(String value) {
        int digits = (int) value.chars().filter(Character::isDigit).count();
        int toMask = Math.max(0, digits - 4);
        StringBuilder masked = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c) && toMask > 0) {
                masked.append('*');
                toMask--;
            } else {
                masked.append(c);
            }
        }
        return masked.toString();
    }

    static int countDigits(String value) {
        return (int) value.chars().filter(Character::isDigit).count();
    }
}
