package com.marinbom2.finbroker.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.marinbom2.finbroker.support.ApiSteps;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** 업체 등록·대조·조회 (FR-CO-001·002). 실제 필터 체인과 MySQL 로 돈다. Docker 가 없으면 건너뛴다 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.secret=test-secret-0123456789-abcdefghijklmnop",
        "security.role.self-change-allowed=true",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CompanyFlowTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    MockMvc mockMvc;

    Cookie admin;
    Cookie user;

    @BeforeEach
    void setUp() throws Exception {
        admin = ApiSteps.admin(mockMvc);
        user = ApiSteps.signupAndLogin(mockMvc);
    }

    @Test
    @DisplayName("운영자 등록 → 확인 전 상태 → 대조(VERIFIED) → 사용자가 상세 조회")
    void registerVerifyAndView() throws Exception {
        String number = uniqueNumber();
        long companyId = register("가나대부", "LENDER", " " + number + " ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationNumber").value(number))
                .andExpect(jsonPath("$.registrationStatus.code").value("UNVERIFIED"))
                .andExpect(jsonPath("$.companyType.description").value("대부업자"))
                .andExpect(jsonPath("$.lastCheckedAt").doesNotExist())
                .returnId();

        check(companyId, "VERIFIED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationStatus.code").value("VERIFIED"))
                .andExpect(jsonPath("$.registrationStatus.description").value("등록 확인"))
                .andExpect(jsonPath("$.lastCheckedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/companies/{id}", companyId).cookie(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("가나대부"))
                .andExpect(jsonPath("$.registrationStatus.code").value("VERIFIED"));
    }

    @Test
    @DisplayName("검색: 상호 일부·등록번호(공백 무시)로 찾고, 상호 가나다순, 페이지는 1부터")
    void search() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        register("다라" + tag, "LENDER", uniqueNumber());
        register("가나" + tag, "LOAN_BROKER", uniqueNumber());
        String number = uniqueNumber();
        register("마바" + tag, "LOAN_AGENT", number);

        mockMvc.perform(get("/api/v1/companies").param("keyword", tag).param("size", "2").cookie(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("가나" + tag))
                .andExpect(jsonPath("$.content[1].name").value("다라" + tag))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.totalSize").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/v1/companies").param("keyword", number.substring(0, 4) + " " + number.substring(4))
                        .cookie(user))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("마바" + tag));
    }

    @Test
    @DisplayName("운영자 목록: 새로 등록한 순, 상태·검색어로 거르고, 지금 남길 수 있는 대조 결과를 함께 준다")
    void opsList() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        long first = register("가나" + tag, "LENDER", uniqueNumber()).returnId();
        long second = register("다라" + tag, "LOAN_BROKER", uniqueNumber()).returnId();
        check(second, "VERIFIED").andExpect(jsonPath("$.allowedResults[*].code").value(
                org.hamcrest.Matchers.contains("VERIFIED", "EXPIRED")));

        mockMvc.perform(get("/api/v1/ops/companies").param("keyword", tag).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].companyId").value(second))
                .andExpect(jsonPath("$.content[0].recheckDue").value(false))
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.content[1].companyId").value(first))
                .andExpect(jsonPath("$.content[1].allowedResults[*].code").value(
                        org.hamcrest.Matchers.contains("VERIFIED", "NOT_FOUND")));

        mockMvc.perform(get("/api/v1/ops/companies").param("keyword", tag).param("status", "UNVERIFIED").cookie(admin))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].companyId").value(first));

        // 방금 확인했으니 90일 기준 재확인 대상이 아니다
        mockMvc.perform(get("/api/v1/ops/companies").param("keyword", tag).param("recheckDue", "true").cookie(admin))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("운영자 현황: 모든 상태가 enum 순서로 나오고, 등록·대조하면 해당 상태 수가 바뀐다")
    void opsSummary() throws Exception {
        long unverifiedBefore = statusCount(0);
        long verifiedBefore = statusCount(1);

        long companyId = register("가나대부", "LENDER", uniqueNumber()).returnId();
        assertThat(statusCount(0)).isEqualTo(unverifiedBefore + 1);

        check(companyId, "VERIFIED");
        assertThat(statusCount(0)).isEqualTo(unverifiedBefore);
        assertThat(statusCount(1)).isEqualTo(verifiedBefore + 1);

        mockMvc.perform(get("/api/v1/ops/companies/summary").cookie(admin))
                .andExpect(jsonPath("$.statuses[*].status.code").value(
                        org.hamcrest.Matchers.contains("UNVERIFIED", "VERIFIED", "NOT_FOUND", "EXPIRED")))
                .andExpect(jsonPath("$.recheckAfterDays").value(90));
    }

    @Test
    @DisplayName("검증: 같은 유형·등록번호 409, 허용되지 않는 전이 409, 없는 업체 404, 페이지 0 은 400")
    void validation() throws Exception {
        String number = uniqueNumber();
        long companyId = register("가나대부", "LENDER", number).returnId();

        register("다른 상호", "LENDER", number)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_EXIST"));
        register("다른 유형", "LOAN_AGENT", number).andExpect(status().isCreated());

        check(companyId, "EXPIRED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANY_STATE_CONFLICT"));
        check(companyId, "UNVERIFIED").andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/companies/{id}", 999_999).cookie(user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_COMPANY"));

        mockMvc.perform(get("/api/v1/companies").param("page", "0").cookie(user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));

        mockMvc.perform(post("/api/v1/ops/companies").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"companyType\":\"LENDER\",\"registrationNumber\":\"1\",\"registrationAuthority\":\"서울\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("권한: 일반 회원은 업체 목록·현황·등록·대조를 쓸 수 없다 (403), 비로그인은 조회도 401")
    void authorization() throws Exception {
        mockMvc.perform(post("/api/v1/ops/companies").cookie(user).contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("가나대부", "LENDER", uniqueNumber())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/ops/companies").cookie(user)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ops/companies/summary").cookie(user)).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isUnauthorized());
    }

    private RegisterResult register(String name, String type, String number) throws Exception {
        return new RegisterResult(mockMvc.perform(post("/api/v1/ops/companies").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content(registerBody(name, type, number))));
    }

    /** 현황에서 index 번째 상태(enum 순서)의 업체 수 */
    private long statusCount(int index) throws Exception {
        String body = mockMvc.perform(get("/api/v1/ops/companies/summary").cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Number count = JsonPath.read(body, "$.statuses[%d].count".formatted(index));
        return count.longValue();
    }

    private ResultActions check(long companyId, String result) throws Exception {
        return mockMvc.perform(patch("/api/v1/ops/companies/{id}/registration-status", companyId).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"%s\"}".formatted(result)));
    }

    private static String registerBody(String name, String type, String number) {
        return """
                {"name":"%s","companyType":"%s","registrationNumber":"%s","registrationAuthority":"서울특별시"}
                """.formatted(name, type, number);
    }

    private static String uniqueNumber() {
        return "2026-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** 응답 검증을 이어 쓰면서 마지막에 업체 ID 를 꺼낸다 */
    private record RegisterResult(ResultActions actions) {

        RegisterResult andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            actions.andExpect(matcher);
            return this;
        }

        long returnId() throws Exception {
            Number id = JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.companyId");
            return id.longValue();
        }
    }
}
