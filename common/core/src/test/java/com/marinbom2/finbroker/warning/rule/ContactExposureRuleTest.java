package com.marinbom2.finbroker.warning.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.marinbom2.finbroker.warning.domain.Warning;
import com.marinbom2.finbroker.warning.domain.WarningSeverity;
import com.marinbom2.finbroker.warning.domain.WarningTarget;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContactExposureRuleTest {

    private final ContactExposureRule rule = new ContactExposureRule();

    @ParameterizedTest(name = "차단: {0}")
    @ValueSource(strings = {
            "연락 주세요 010-1234-5678",
            "01012345678 로 문자",
            "사무실 02-123-4567",
            "주민번호 900101-1234567",
            "계좌 110-123-456789 로",
            "카톡 아이디 loan_king99",
            "텔레그램 @fastmoney",
            "open.kakao.com/o/abc123 여기로",
    })
    void blocks(String text) {
        List<Warning> warnings = inspect(text);

        assertThat(warnings).isNotEmpty();
        assertThat(warnings).allMatch(w -> w.severity() == WarningSeverity.BLOCK);
    }

    @ParameterizedTest(name = "통과: {0}")
    @ValueSource(strings = {
            "2026-01-05 에 상담받았어요",
            "300만원 빌렸고 12개월 상환",
            "카톡으로 연락 왔어요",
            "등록번호 2026-서울강남-0001 업체",
    })
    void passes(String text) {
        assertThat(inspect(text)).isEmpty();
    }

    @Test
    @DisplayName("근거에는 가린 값만 들어간다 (원문 번호를 되돌려 보내지 않는다)")
    void masksEvidence() {
        assertThat(inspect("010-1234-5678").getFirst().evidence()).isEqualTo("***-****-5678");
        assertThat(inspect("카톡 loan_king99").getFirst().evidence()).isEqualTo("카톡 lo*********");
    }

    private List<Warning> inspect(String text) {
        return rule.inspect(new WarningTarget(text, null));
    }
}
