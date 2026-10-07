package com.example.delivery.global.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.support.ApiTestSupport;
import com.example.delivery.user.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 필터와 Security 설정 검증. 메뉴·주문 API가 생기기 전이므로 로그인이 필요한 테스트 전용 엔드포인트로 확인한다.
 */
@Import(SecurityApiTest.AuthUserEchoController.class)
class SecurityApiTest extends ApiTestSupport {

    @Autowired
    private JwtProvider jwtProvider;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    @DisplayName("유효한 토큰이면 토큰의 회원 정보로 인증된다")
    void validToken() throws Exception {
        // given
        String token = jwtProvider.createToken(1L, "owner1", UserRole.OWNER);

        // when
        ResultActions result = mockMvc.perform(get("/api/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("owner1"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    @DisplayName("토큰 없이 로그인이 필요한 API를 호출하면 403")
    void noToken() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/test/me"));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("변조된 토큰은 500이 아니라 인증 없는 요청으로 처리되어 403")
    void invalidToken() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("만료된 토큰은 인증 없는 요청으로 처리되어 403")
    void expiredToken() throws Exception {
        // given
        JwtProvider pastProvider = new JwtProvider(secret, Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC));
        String expiredToken = pastProvider.createToken(1L, "owner1", UserRole.OWNER);

        // when
        ResultActions result = mockMvc.perform(get("/api/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("잘못된 토큰이 있어도 공개 API는 거절하지 않는다")
    void invalidTokenOnPublicApi() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/auth/signup")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "owner1", "password": "password123", "role": "OWNER"}
                        """));

        // then
        result.andExpect(status().isCreated());
    }

    @Test
    @DisplayName("토큰을 가진 POST 요청이 CSRF로 막히지 않는다")
    void postWithTokenNotBlockedByCsrf() throws Exception {
        // given
        String token = jwtProvider.createToken(1L, "owner1", UserRole.OWNER);

        // when
        ResultActions result = mockMvc.perform(post("/api/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("400 에러가 본문 없는 403으로 바뀌지 않고 에러 형식으로 응답된다")
    void errorResponseNotReplacedBy403() throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @RestController
    static class AuthUserEchoController {

        @GetMapping("/api/test/me")
        AuthUser me(@AuthenticationPrincipal AuthUser authUser) {
            return authUser;
        }

        @PostMapping("/api/test/me")
        AuthUser postMe(@AuthenticationPrincipal AuthUser authUser) {
            return authUser;
        }
    }
}
