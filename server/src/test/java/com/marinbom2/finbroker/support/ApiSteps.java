package com.marinbom2.finbroker.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 통합 테스트가 같이 쓰는 API 호출 단계. 실제 필터 체인을 지난다.
 * 권한 바꾸기를 쓰려면 테스트에 security.role.self-change-allowed=true 가 있어야 한다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiSteps {

    public static final String PASSWORD = "Passw0rd!";
    public static final String COOKIE = "finbroker_at";

    /** 새 회원으로 가입·로그인하고 인증 쿠키를 돌려준다 */
    public static Cookie signupAndLogin(MockMvc mockMvc) throws Exception {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(COOKIE);
    }

    /** 새 회원을 만들어 운영자(ADMIN)로 바꾼다 */
    public static Cookie admin(MockMvc mockMvc) throws Exception {
        Cookie cookie = signupAndLogin(mockMvc);
        mockMvc.perform(patch("/api/v1/me/role").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleType\":\"ADMIN\"}"))
                .andExpect(status().isOk());
        return cookie;
    }
}
