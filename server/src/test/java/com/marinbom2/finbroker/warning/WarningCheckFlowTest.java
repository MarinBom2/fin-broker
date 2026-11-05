package com.marinbom2.finbroker.warning;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/** 피해 예방 검사 API (FR-WN-001). 규칙 판정 세부는 core 의 규칙 테스트가 본다 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.secret=test-secret-0123456789-abcdefghijklmnop",
        "security.role.self-change-allowed=true",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class WarningCheckFlowTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    MockMvc mockMvc;

    Cookie user;

    @BeforeEach
    void setUp() throws Exception {
        user = ApiSteps.signupAndLogin(mockMvc);
    }

    @Test
    @DisplayName("사기 문자: 선입금·최고금리 초과·통장 요구를 모두 찾는다. 경고만 있으면 blocked=false")
    void scamMessage() throws Exception {
        check(null, "선입금 30만원 보내 주시면 월 3%로 바로 승인. 체크카드는 택배로 보내 주세요", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.warnings[*].type.code",
                        org.hamcrest.Matchers.hasItems("OVER_INTEREST_CAP", "UPFRONT_PAYMENT", "ACCOUNT_HANDOVER")))
                .andExpect(jsonPath("$.warnings[0].type.severity.code").value("WARN"))
                .andExpect(jsonPath("$.warnings[0].type.guidance").isNotEmpty());
    }

    @Test
    @DisplayName("연락처: blocked=true, BLOCK 이 먼저 오고, 응답에 원문 번호가 없다")
    void contactBlocked() throws Exception {
        check(null, "선입금 필요, 010-9876-5432 로 연락", null)
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.warnings[0].type.code").value("CONTACT_EXPOSURE"))
                .andExpect(jsonPath("$.warnings[0].evidence").value("***-****-5432"))
                .andExpect(content().string(not(containsString("9876"))));
    }

    @Test
    @DisplayName("업체: 확인 전 업체를 가리키면 UNREGISTERED_COMPANY, 대조를 마치면 사라진다. 없는 업체는 404")
    void company() throws Exception {
        Cookie admin = ApiSteps.admin(mockMvc);
        String body = """
                {"name":"가나대부","companyType":"LENDER","registrationNumber":"2026-%s","registrationAuthority":"서울특별시"}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
        String created = mockMvc.perform(post("/api/v1/ops/companies").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        long companyId = ((Number) JsonPath.read(created, "$.companyId")).longValue();

        check("후기", "여기서 빌렸어요", companyId)
                .andExpect(jsonPath("$.warnings[0].type.code").value("UNREGISTERED_COMPANY"))
                .andExpect(jsonPath("$.warnings[0].evidence").value("가나대부 (확인 전)"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/ops/companies/{id}/registration-status", companyId).cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"VERIFIED\"}"))
                .andExpect(status().isOk());

        check("후기", "여기서 빌렸어요", companyId)
                .andExpect(jsonPath("$.warnings.length()").value(0));

        check(null, "내용", 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_COMPANY"));
    }

    @Test
    @DisplayName("빈 본문은 400, 비로그인은 401")
    void validation() throws Exception {
        check(null, " ", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("body"));

        mockMvc.perform(post("/api/v1/warnings/check").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions check(String title, String body, Long companyId) throws Exception {
        String json = """
                {"title":%s,"body":"%s","companyId":%s}
                """.formatted(title == null ? "null" : "\"" + title + "\"", body, companyId);
        return mockMvc.perform(post("/api/v1/warnings/check").cookie(user)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
