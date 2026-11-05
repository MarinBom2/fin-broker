package com.marinbom2.finbroker.company.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marinbom2.finbroker.company.domain.exception.IllegalCompanyStateException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompanyTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("등록: 확인 전(UNVERIFIED)으로 시작하고, 등록번호의 공백은 지운다 (FR-CO-002)")
    void register() {
        Company company = Company.register(" 가나대부 ", CompanyType.LENDER, " 2026-서울-0001 ", " 서울특별시 ");

        assertThat(company.getName()).isEqualTo("가나대부");
        assertThat(company.getRegistrationNumber()).isEqualTo("2026-서울-0001");
        assertThat(company.getRegistrationAuthority()).isEqualTo("서울특별시");
        assertThat(company.getRegistrationStatus()).isEqualTo(RegistrationStatus.UNVERIFIED);
        assertThat(company.isVerified()).isFalse();
        assertThat(company.getLastCheckedAt()).isNull();
    }

    @Test
    @DisplayName("대조 결과를 남기면 상태·시각·운영자가 함께 바뀐다")
    void recordCheck() {
        Company company = company();

        company.recordCheck(RegistrationStatus.VERIFIED, 9L, NOW);

        assertThat(company.isVerified()).isTrue();
        assertThat(company.getLastCheckedAt()).isEqualTo(NOW);
        assertThat(company.getCheckedBy()).isEqualTo(9L);
    }

    @ParameterizedTest(name = "{0} -> {1} 허용")
    @CsvSource({
            "UNVERIFIED, VERIFIED", "UNVERIFIED, NOT_FOUND",
            "VERIFIED, VERIFIED", "VERIFIED, EXPIRED",
            "NOT_FOUND, VERIFIED", "NOT_FOUND, NOT_FOUND",
            "EXPIRED, VERIFIED", "EXPIRED, EXPIRED",
    })
    @DisplayName("전이 표에 있는 대조 결과는 허용한다 (md/domain-design.md §4.1)")
    void allowedTransitions(RegistrationStatus from, RegistrationStatus to) {
        assertThat(from.canMoveTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} 거부")
    @CsvSource({
            "UNVERIFIED, UNVERIFIED", "UNVERIFIED, EXPIRED",
            "VERIFIED, UNVERIFIED", "VERIFIED, NOT_FOUND",
            "NOT_FOUND, UNVERIFIED", "NOT_FOUND, EXPIRED",
            "EXPIRED, UNVERIFIED", "EXPIRED, NOT_FOUND",
    })
    @DisplayName("전이 표에 없는 대조 결과는 IllegalCompanyStateException, 상태는 그대로")
    void rejectedTransitions(RegistrationStatus from, RegistrationStatus to) {
        Company company = companyIn(from);

        assertThatThrownBy(() -> company.recordCheck(to, 9L, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalCompanyStateException.class);
        assertThat(company.getRegistrationStatus()).isEqualTo(from);
    }

    @Test
    @DisplayName("상태마다 남길 수 있는 대조 결과 (운영자 버튼). 확인 전으로 되돌리는 결과는 없다")
    void allowedResults() {
        assertThat(RegistrationStatus.UNVERIFIED.allowedResults())
                .containsExactly(RegistrationStatus.VERIFIED, RegistrationStatus.NOT_FOUND);
        assertThat(RegistrationStatus.VERIFIED.allowedResults())
                .containsExactly(RegistrationStatus.VERIFIED, RegistrationStatus.EXPIRED);
        for (RegistrationStatus status : RegistrationStatus.values()) {
            assertThat(status.allowedResults()).doesNotContain(RegistrationStatus.UNVERIFIED);
        }
    }

    @Test
    @DisplayName("재확인 필요: 등록 확인한 지 기간이 지난 업체만 (경계값 포함). 확인 전·정보 없음은 대상이 아니다")
    void needsRecheck() {
        Duration ninetyDays = Duration.ofDays(90);
        Company verified = companyIn(RegistrationStatus.VERIFIED);

        assertThat(verified.needsRecheck(ninetyDays, NOW.plus(Duration.ofDays(89)))).isFalse();
        assertThat(verified.needsRecheck(ninetyDays, NOW.plus(ninetyDays))).isTrue();
        assertThat(company().needsRecheck(ninetyDays, NOW.plus(Duration.ofDays(365)))).isFalse();
        assertThat(companyIn(RegistrationStatus.NOT_FOUND).needsRecheck(ninetyDays, NOW.plus(Duration.ofDays(365)))).isFalse();
    }

    private static Company company() {
        return Company.register("가나대부", CompanyType.LENDER, "2026-서울-0001", "서울특별시");
    }

    /** 허용된 전이로만 원하는 상태를 만든다 */
    private static Company companyIn(RegistrationStatus status) {
        Company company = company();
        switch (status) {
            case UNVERIFIED -> { }
            case VERIFIED -> company.recordCheck(RegistrationStatus.VERIFIED, 1L, NOW);
            case NOT_FOUND -> company.recordCheck(RegistrationStatus.NOT_FOUND, 1L, NOW);
            case EXPIRED -> {
                company.recordCheck(RegistrationStatus.VERIFIED, 1L, NOW);
                company.recordCheck(RegistrationStatus.EXPIRED, 1L, NOW);
            }
        }
        return company;
    }
}
