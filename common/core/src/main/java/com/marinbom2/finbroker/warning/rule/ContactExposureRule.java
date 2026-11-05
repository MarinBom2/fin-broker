package com.marinbom2.finbroker.warning.rule;

import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningRule;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import com.marinbom2.finbroker.warning.domain.WarningType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 연락처·개인정보 노출 (BLOCK). 글로 고객이 업체에 바로 넘어가는 통로를 막고, 작성자 개인정보를 지킨다 (md/domain-design.md §4.3).
 * 근거 조각은 가린 값만 돌려준다. 원문 번호를 응답에 되돌려 보내지 않는다.
 */
@Component
public class ContactExposureRule implements WarningRule {

    /** 휴대전화 */
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)01[016789][-.\\s]?\\d{3,4}[-.\\s]?\\d{4}(?!\\d)");
    /** 지역번호·인터넷 전화 */
    private static final Pattern LANDLINE = Pattern.compile("(?<!\\d)0(?:2|[3-6][1-5]|70)[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}(?!\\d)");
    /** 주민등록번호 */
    private static final Pattern RESIDENT_NUMBER = Pattern.compile("(?<!\\d)\\d{6}[-\\s]?[1-4]\\d{6}(?!\\d)");
    /** 계좌번호: 하이픈으로 나뉜 숫자 묶음. 날짜(2026-01-01)와 구분하려고 숫자가 10개 이상일 때만 */
    private static final Pattern ACCOUNT = Pattern.compile("(?<![\\d-])\\d{2,6}-\\d{2,6}-\\d{2,8}(?:-\\d{1,4})?(?![\\d-])");
    private static final int MIN_ACCOUNT_DIGITS = 10;
    /** 메신저 아이디 */
    private static final Pattern MESSENGER_ID = Pattern.compile(
            "(?i)(카카오톡|카톡|텔레그램|텔레|라인|위챗)\\s*(?:아이디|id)?\\s*[:：]?\\s*@?([a-z0-9_.]{4,})");
    /** 오픈채팅·메신저 링크 */
    private static final Pattern MESSENGER_LINK = Pattern.compile("(?i)(open\\.kakao\\.com|t\\.me)/\\S+");

    @Override
    public List<Warning> inspect(WarningTarget target) {
        String text = target.text();
        List<Warning> warnings = new ArrayList<>();

        addMaskedNumbers(text, MOBILE, 0, warnings);
        addMaskedNumbers(text, LANDLINE, 0, warnings);
        addMaskedNumbers(text, RESIDENT_NUMBER, 0, warnings);
        addMaskedNumbers(text, ACCOUNT, MIN_ACCOUNT_DIGITS, warnings);

        Matcher id = MESSENGER_ID.matcher(text);
        while (id.find()) {
            warnings.add(warning(id.group(1) + " " + maskId(id.group(2))));
        }
        Matcher link = MESSENGER_LINK.matcher(text);
        while (link.find()) {
            warnings.add(warning(link.group(1) + "/…"));
        }
        return warnings;
    }

    private static void addMaskedNumbers(String text, Pattern pattern, int minDigits, List<Warning> warnings) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String found = matcher.group();
            if (PatternRules.countDigits(found) >= minDigits) {
                warnings.add(warning(PatternRules.maskDigits(found)));
            }
        }
    }

    /** 앞 두 글자만 남긴다 */
    private static String maskId(String id) {
        return id.substring(0, 2) + "*".repeat(id.length() - 2);
    }

    private static Warning warning(String evidence) {
        return new Warning(WarningType.CONTACT_EXPOSURE, evidence);
    }
}
