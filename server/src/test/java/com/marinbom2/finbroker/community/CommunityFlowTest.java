package com.marinbom2.finbroker.community;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** 커뮤니티 글·답변 (FR-CM-001~003, FR-WN-002). 실제 필터 체인과 MySQL. Docker 가 없으면 건너뛴다 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.secret=test-secret-0123456789-abcdefghijklmnop",
        "security.role.self-change-allowed=true",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CommunityFlowTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    MockMvc mockMvc;

    Cookie writer;
    Cookie reader;

    @BeforeEach
    void setUp() throws Exception {
        writer = ApiSteps.signupAndLogin(mockMvc);
        reader = ApiSteps.signupAndLogin(mockMvc);
    }

    @Test
    @DisplayName("글쓰기 → 목록(최신순·필터) → 상세. 작성자는 역할로만 보이고 이메일은 나가지 않는다")
    void writeListDetail() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        long first = id(write(writer, "QUESTION", "CREDIT", null, "질문 " + tag, "신용대출 한도가 궁금해요")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.authorRole.code").value("USER"))
                .andExpect(jsonPath("$.warnings.length()").value(0))
                .andExpect(jsonPath("$.answers.length()").value(0)));
        long second = id(write(writer, "REVIEW", "SECURED", null, "후기 " + tag, "담보대출 받아 본 후기입니다"));

        mockMvc.perform(get("/api/v1/posts").param("size", "50").cookie(reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(second))
                .andExpect(jsonPath("$.content[1].postId").value(first))
                .andExpect(jsonPath("$.content[0].postType.description").value("후기"));

        mockMvc.perform(get("/api/v1/posts").param("postType", "QUESTION").param("loanType", "CREDIT").cookie(reader))
                .andExpect(jsonPath("$.content[*].postType.code", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is("QUESTION"))));

        mockMvc.perform(get("/api/v1/posts/{id}", first).cookie(reader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("질문 " + tag))
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(content().string(not(containsString("@example.com"))));
    }

    @Test
    @DisplayName("주의 신호는 글에 남는다. 연락처가 있으면 400 CONTENT_BLOCKED + warnings, 저장되지 않는다")
    void warnings() throws Exception {
        long warned = id(write(writer, "SCAM_CASE", "CREDIT", null, "이런 문자 받았어요", "선입금 30만원 보내면 월 3%로 해 준대요")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings[*].code", org.hamcrest.Matchers.containsInAnyOrder(
                        "OVER_INTEREST_CAP", "UPFRONT_PAYMENT")))
                .andExpect(jsonPath("$.warnings[0].guidance").isNotEmpty()));

        mockMvc.perform(get("/api/v1/posts").cookie(reader))
                .andExpect(jsonPath("$.content[?(@.postId == %d)].warnings.length()".formatted(warned)).value(2));

        write(writer, "QUESTION", "CREDIT", null, "연락 주세요", "010-9876-5432 로 문자 주세요")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_BLOCKED"))
                .andExpect(jsonPath("$.warnings[0].type.code").value("CONTACT_EXPOSURE"))
                .andExpect(jsonPath("$.warnings[0].evidence").value("***-****-5432"))
                .andExpect(content().string(not(containsString("9876"))));
    }

    @Test
    @DisplayName("답변: 답변 수가 오르고, 글쓴이 답변은 byPostAuthor. 연락처 답변은 400")
    void answers() throws Exception {
        long postId = id(write(writer, "QUESTION", "POLICY", null, "햇살론 문의", "조건이 어떻게 되나요"));

        answer(reader, postId, "공식 상담 창구에 먼저 물어보세요")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.byPostAuthor").value(false))
                .andExpect(jsonPath("$.mine").value(true));
        answer(writer, postId, "감사합니다")
                .andExpect(jsonPath("$.byPostAuthor").value(true));
        answer(reader, postId, "카톡 loan_king99 로 연락 주세요")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_BLOCKED"));

        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(reader))
                .andExpect(jsonPath("$.answers.length()").value(2))
                .andExpect(jsonPath("$.answers[0].body").value("공식 상담 창구에 먼저 물어보세요"))
                .andExpect(jsonPath("$.answers[0].mine").value(true))
                .andExpect(jsonPath("$.answers[1].byPostAuthor").value(true));
        mockMvc.perform(get("/api/v1/posts").param("postType", "QUESTION").param("size", "50").cookie(reader))
                .andExpect(jsonPath("$.content[?(@.postId == %d)].answerCount".formatted(postId)).value(2));
    }

    @Test
    @DisplayName("업체를 가리키면 업체 요약이 붙고, 확인 전 업체면 UNREGISTERED_COMPANY 신호. 없는 업체·글은 404")
    void companyAndNotFound() throws Exception {
        Cookie admin = ApiSteps.admin(mockMvc);
        String created = mockMvc.perform(post("/api/v1/ops/companies").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"가나대부","companyType":"LENDER","registrationNumber":"2026-%s","registrationAuthority":"서울특별시"}
                                """.formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andReturn().getResponse().getContentAsString();
        long companyId = ((Number) JsonPath.read(created, "$.companyId")).longValue();

        write(writer, "REVIEW", "CREDIT", companyId, "가나대부 후기", "상담 친절했어요")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company.name").value("가나대부"))
                .andExpect(jsonPath("$.company.registrationStatus.code").value("UNVERIFIED"))
                .andExpect(jsonPath("$.warnings[0].code").value("UNREGISTERED_COMPANY"));

        write(writer, "REVIEW", "CREDIT", 999_999L, "없는 업체", "내용")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_COMPANY"));
        mockMvc.perform(get("/api/v1/posts/{id}", 999_999).cookie(reader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_POST"));
        answer(reader, 999_999L, "답변").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("검증: 제목 없음·유형 없음 400 + errors, 비로그인 401")
    void validation() throws Exception {
        mockMvc.perform(post("/api/v1/posts").cookie(writer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postType\":\"QUESTION\",\"title\":\"\",\"body\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATA"))
                .andExpect(jsonPath("$.errors.length()").value(2));
        mockMvc.perform(get("/api/v1/posts")).andExpect(status().isUnauthorized());
    }

    private ResultActions write(Cookie cookie, String postType, String loanType, Long companyId, String title, String body)
            throws Exception {
        String json = """
                {"postType":"%s","loanType":"%s","companyId":%s,"title":"%s","body":"%s"}
                """.formatted(postType, loanType, companyId, title, body);
        return mockMvc.perform(post("/api/v1/posts").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions answer(Cookie cookie, long postId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/posts/{id}/answers", postId).cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"%s\"}".formatted(body)));
    }

    private static long id(ResultActions actions) throws Exception {
        Number id = JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.postId");
        return id.longValue();
    }
}
