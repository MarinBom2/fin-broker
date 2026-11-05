package com.marinbom2.finbroker.warning.rule;

import com.marinbom2.finbroker.warning.WarningProperties;
import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningRule;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 법정 최고금리 초과 금리 표기 (WARN). 월·주·일 금리는 연 금리로 바꿔 비교한다 (단리 기준 환산).
 * 숫자 판정은 이 규칙이 한다. LLM 에 맡기지 않는다 (md/domain-design.md §6).
 */
@Component
public class InterestCapRule implements WarningRule {

    private static final String RATE = "(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:%|퍼센트|프로)";
    private static final String KEYWORD = "(?:이자율|이자|이율|금리)";

    /** 기간이 붙은 금리: "연 30%", "월이자 3%", "매일 1%". "30일 5%"·"1년 5%"처럼 숫자 뒤의 기간은 금리 기간이 아니다 */
    private static final Pattern PERIOD_RATE = Pattern.compile(
            "(?<![0-9가-힣])매?(연|년|월|주|일)\\s*" + KEYWORD + "?\\s*" + RATE);
    /** 기간 없이 금리만: "금리 30%". 연 금리로 본다 */
    private static final Pattern PLAIN_RATE = Pattern.compile(
            KEYWORD + "\\s*(?:은|는|이|가|도)?\\s*" + RATE);

    private final BigDecimal capPercent;

    public InterestCapRule(WarningProperties properties) {
        this.capPercent = properties.interestCapPercent();
    }

    @Override
    public List<Warning> inspect(WarningTarget target) {
        String text = target.text();
        List<Warning> warnings = new ArrayList<>();
        List<int[]> periodRanges = new ArrayList<>();

        Matcher period = PERIOD_RATE.matcher(text);
        while (period.find()) {
            periodRanges.add(new int[] {period.start(), period.end()});
            BigDecimal annual = new BigDecimal(period.group(2)).multiply(multiplier(period.group(1)));
            addIfOverCap(period.group(), annual, !isAnnual(period.group(1)), warnings);
        }

        Matcher plain = PLAIN_RATE.matcher(text);
        while (plain.find()) {
            if (overlaps(plain.start(), plain.end(), periodRanges)) {
                continue; // "연 금리 30%" 는 위에서 이미 봤다
            }
            addIfOverCap(plain.group(), new BigDecimal(plain.group(1)), false, warnings);
        }
        return warnings;
    }

    private void addIfOverCap(String found, BigDecimal annual, boolean converted, List<Warning> warnings) {
        if (annual.compareTo(capPercent) <= 0) {
            return;
        }
        String evidence = converted ? found + " (연 " + annual.stripTrailingZeros().toPlainString() + "%로 환산)" : found;
        warnings.add(new Warning(WarningType.OVER_INTEREST_CAP, evidence));
    }

    private static boolean isAnnual(String period) {
        return "연".equals(period) || "년".equals(period);
    }

    private static BigDecimal multiplier(String period) {
        return switch (period) {
            case "월" -> BigDecimal.valueOf(12);
            case "주" -> BigDecimal.valueOf(52);
            case "일" -> BigDecimal.valueOf(365);
            default -> BigDecimal.ONE;
        };
    }

    private static boolean overlaps(int start, int end, List<int[]> ranges) {
        return ranges.stream().anyMatch(r -> start < r[1] && r[0] < end);
    }
}
