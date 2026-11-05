package com.marinbom2.finbroker.company;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.marinbom2.finbroker.support.ApiSteps;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/** 재확인 필요 (SCR-08). 기간을 0 으로 두어, 확인한 시각이 곧 경계값인 경우를 본다 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.secret=test-secret-0123456789-abcdefghijklmnop",
        "security.role.self-change-allowed=true",
        "company.recheck-after=0s",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CompanyRecheckFlowTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("등록 확인한 업체만 재확인 필요로 보이고, 걸러 보기·현황 수가 같은 기준을 쓴다")
    void recheckDue() throws Exception {
        Cookie admin = ApiSteps.admin(mockMvc);
        String tag = UUID.randomUUID().toString().substring(0, 6);
        long verified = register(admin, "가나" + tag);
        long unverified = register(admin, "다라" + tag);
        check(admin, verified);

        mockMvc.perform(get("/api/v1/ops/companies").param("keyword", tag).cookie(admin))
                .andExpect(jsonPath("$.content[?(@.companyId == %d)].recheckDue".formatted(verified)).value(true))
                .andExpect(jsonPath("$.content[?(@.companyId == %d)].recheckDue".formatted(unverified)).value(false));

        mockMvc.perform(get("/api/v1/ops/companies").param("keyword", tag).param("recheckDue", "true").cookie(admin))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].companyId").value(verified));

        mockMvc.perform(get("/api/v1/ops/companies/summary").cookie(admin))
                .andExpect(jsonPath("$.recheckDue").value(1))
                .andExpect(jsonPath("$.recheckAfterDays").value(0));
    }

    private long register(Cookie admin, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/ops/companies").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","companyType":"LENDER","registrationNumber":"2026-%s","registrationAuthority":"서울특별시"}
                                """.formatted(name, UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.companyId");
        return id.longValue();
    }

    private void check(Cookie admin, long companyId) throws Exception {
        mockMvc.perform(patch("/api/v1/ops/companies/{id}/registration-status", companyId).cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"VERIFIED\"}"))
                .andExpect(status().isOk());
    }
}
