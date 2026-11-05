package com.marinbom2.finbroker.moderation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.marinbom2.finbroker.support.ApiSteps;
import jakarta.servlet.http.Cookie;
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

/**
 * 글·답변 수정·삭제 (FR-CM-001·003)와 신고·운영자 처리 (FR-MD-001·002). 자동 숨김 기준은 2로 낮춰 둔다.
 * 실제 필터 체인과 MySQL. Docker 가 없으면 건너뛴다.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.secret=test-secret-0123456789-abcdefghijklmnop",
        "security.role.self-change-allowed=true",
        "moderation.auto-hide-report-count=2",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ModerationFlowTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    MockMvc mockMvc;

    Cookie writer;
    Cookie other;

    @BeforeEach
    void setUp() throws Exception {
        writer = ApiSteps.signupAndLogin(mockMvc);
        other = ApiSteps.signupAndLogin(mockMvc);
    }

    @Test
    @DisplayName("글 고치기: 작성자만(아니면 403), 주의 신호는 다시 검사, 연락처를 넣으면 400 이고 내용은 그대로")
    void editPost() throws Exception {
        long postId = writePost(writer, "선입금 문의", "선입금 보내라고 해요");

        editPost(other, postId, "남의 글", "고치기")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        editPost(writer, postId, "고친 제목", "정상적인 상담 후기입니다")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("고친 제목"))
                .andExpect(jsonPath("$.warnings.length()").value(0));

        editPost(writer, postId, "연락처", "010-1234-5678")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONTENT_BLOCKED"));
        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(other))
                .andExpect(jsonPath("$.title").value("고친 제목"));
    }

    @Test
    @DisplayName("글 지우기: 작성자만, 지운 글은 404 이고 목록에서 사라진다")
    void deletePost() throws Exception {
        long postId = writePost(writer, "지울 글", "내용");

        mockMvc.perform(delete("/api/v1/posts/{id}", postId).cookie(other)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/posts/{id}", postId).cookie(writer)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(writer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND_POST"));
        mockMvc.perform(get("/api/v1/posts").param("size", "50").cookie(writer))
                .andExpect(jsonPath("$.content[?(@.postId == %d)]".formatted(postId)).isEmpty());
    }

    @Test
    @DisplayName("답변 고치기·지우기: 작성자만, 지우면 답변 수가 내려가고 상세에서 사라진다")
    void editAndDeleteAnswer() throws Exception {
        long postId = writePost(writer, "질문", "내용");
        long answerId = answer(other, postId, "첫 답변");

        mockMvc.perform(patchJson("/api/v1/posts/{p}/answers/{a}", writer, "{\"body\":\"남의 답변\"}", postId, answerId))
                .andExpect(status().isForbidden());
        mockMvc.perform(patchJson("/api/v1/posts/{p}/answers/{a}", other, "{\"body\":\"고친 답변\"}", postId, answerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("고친 답변"));

        mockMvc.perform(delete("/api/v1/posts/{p}/answers/{a}", postId, answerId).cookie(other))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(writer))
                .andExpect(jsonPath("$.answers.length()").value(0));
        mockMvc.perform(get("/api/v1/posts").param("size", "50").cookie(writer))
                .andExpect(jsonPath("$.content[?(@.postId == %d)].answerCount".formatted(postId)).value(0));
    }

    @Test
    @DisplayName("신고: 내 글은 400, 같은 대상 두 번은 409, 상세에 reportedByMe")
    void fileReport() throws Exception {
        long postId = writePost(writer, "광고 글", "우리 업체로 오세요");

        report(writer, "POST", postId, "PROMOTION")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_REPORT"));
        report(other, "POST", postId, "PROMOTION")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status.code").value("OPEN"))
                .andExpect(jsonPath("$.targetHidden").value(false));
        report(other, "POST", postId, "SCAM")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REPORTED"));

        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(other))
                .andExpect(jsonPath("$.reportedByMe").value(true));
        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(writer))
                .andExpect(jsonPath("$.reportedByMe").value(false));
        report(other, "POST", 999_999L, "SCAM").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("신고 누적: 서로 다른 회원 2명이 신고하면 자동으로 숨겨진다 → 운영자가 문제없음으로 처리하면 되살아난다")
    void autoHideThenDismiss() throws Exception {
        long postId = writePost(writer, "논란 글", "내용");
        Cookie third = ApiSteps.signupAndLogin(mockMvc);

        report(other, "POST", postId, "ABUSE").andExpect(jsonPath("$.targetHidden").value(false));
        long reportId = id(report(third, "POST", postId, "ABUSE").andExpect(jsonPath("$.targetHidden").value(true)), "$.reportId");
        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(writer)).andExpect(status().isNotFound());

        Cookie admin = ApiSteps.admin(mockMvc);
        mockMvc.perform(get("/api/v1/ops/reports").param("status", "OPEN").param("size", "50").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.postId == %d)]".formatted(postId), org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[?(@.reportId == %d)].reportCount".formatted(reportId)).value(2))
                .andExpect(jsonPath("$.content[?(@.reportId == %d)].reasons[0].code".formatted(reportId)).value("ABUSE"))
                .andExpect(jsonPath("$.content[?(@.reportId == %d)].targetStatus.code".formatted(reportId)).value("HIDDEN"))
                .andExpect(jsonPath("$.content[?(@.reportId == %d)].postTitle".formatted(reportId)).value("논란 글"));

        decide(admin, reportId, "DISMISSED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("DISMISSED"))
                .andExpect(jsonPath("$.reportCount").value(2))
                .andExpect(jsonPath("$.targetStatus.code").value("PUBLISHED"));
        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(writer)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ops/reports").param("status", "OPEN").param("size", "50").cookie(admin))
                .andExpect(jsonPath("$.content[?(@.postId == %d)]".formatted(postId)).isEmpty());
    }

    @Test
    @DisplayName("운영자 숨김 처리: 답변 신고를 ACCEPTED 로 처리하면 답변이 숨겨지고 답변 수가 내려간다. 일반 회원은 403")
    void acceptAnswerReport() throws Exception {
        long postId = writePost(writer, "질문", "내용");
        long answerId = answer(other, postId, "홍보성 답변");
        long reportId = id(report(writer, "ANSWER", answerId, "PROMOTION").andExpect(status().isCreated()), "$.reportId");

        decide(other, reportId, "ACCEPTED").andExpect(status().isForbidden());

        Cookie admin = ApiSteps.admin(mockMvc);
        decide(admin, reportId, "ACCEPTED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("ACCEPTED"))
                .andExpect(jsonPath("$.targetType.code").value("ANSWER"))
                .andExpect(jsonPath("$.targetStatus.code").value("HIDDEN"));
        mockMvc.perform(get("/api/v1/posts/{id}", postId).cookie(writer))
                .andExpect(jsonPath("$.answers.length()").value(0));
        decide(admin, reportId, "OPEN").andExpect(status().isBadRequest());
    }

    private long writePost(Cookie cookie, String title, String body) throws Exception {
        return id(mockMvc.perform(post("/api/v1/posts").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"postType":"QUESTION","loanType":"CREDIT","companyId":null,"title":"%s","body":"%s"}
                                """.formatted(title, body)))
                .andExpect(status().isCreated()), "$.postId");
    }

    private ResultActions editPost(Cookie cookie, long postId, String title, String body) throws Exception {
        return mockMvc.perform(patchJson("/api/v1/posts/{id}", cookie, """
                {"postType":"REVIEW","loanType":"CREDIT","companyId":null,"title":"%s","body":"%s"}
                """.formatted(title, body), postId));
    }

    private long answer(Cookie cookie, long postId, String body) throws Exception {
        return id(mockMvc.perform(post("/api/v1/posts/{id}/answers", postId).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"%s\"}".formatted(body)))
                .andExpect(status().isCreated()), "$.answerId");
    }

    private ResultActions report(Cookie cookie, String targetType, long targetId, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/reports").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"%s\",\"targetId\":%d,\"reason\":\"%s\"}".formatted(targetType, targetId, reason)));
    }

    private ResultActions decide(Cookie cookie, long reportId, String decision) throws Exception {
        return mockMvc.perform(patchJson("/api/v1/ops/reports/{id}", cookie,
                "{\"decision\":\"%s\"}".formatted(decision), reportId));
    }

    private static org.springframework.test.web.servlet.RequestBuilder patchJson(String path, Cookie cookie, String json,
                                                                               Object... vars) {
        return patch(path, vars).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static long id(ResultActions actions, String path) throws Exception {
        Number id = JsonPath.read(actions.andReturn().getResponse().getContentAsString(), path);
        return id.longValue();
    }
}
