package com.example.delivery.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class UserApiTest extends ApiTestSupport {

    @Nested
    @DisplayName("회원가입 POST /api/auth/signup")
    class Signup {

        @Test
        @DisplayName("가입하면 201과 회원 정보를 응답한다 (시나리오 #1)")
        void success() throws Exception {
            // when
            ResultActions result = signup("owner1", "password123", "OWNER");

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").isNumber())
                    .andExpect(jsonPath("$.username").value("owner1"))
                    .andExpect(jsonPath("$.role").value("OWNER"))
                    .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
        }

        @Test
        @DisplayName("응답에 비밀번호를 담지 않는다")
        void responseHasNoPassword() throws Exception {
            // when
            ResultActions result = signup("cust1", "password123", "CUSTOMER");

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("비밀번호는 $2로 시작하는 BCrypt 해시로 저장된다")
        void passwordStoredAsBcrypt() throws Exception {
            // given
            signup("owner1", "password123", "OWNER");

            // when
            String stored = jdbcTemplate.queryForObject(
                    "SELECT password FROM users WHERE username = 'owner1'", String.class);

            // then
            assertThat(stored).startsWith("$2").isNotEqualTo("password123");
        }

        @Test
        @DisplayName("이미 있는 아이디로 가입하면 409 DUPLICATE_USERNAME (시나리오 #5)")
        void duplicateUsername() throws Exception {
            // given
            signup("owner1", "password123", "OWNER");

            // when
            ResultActions result = signup("owner1", "otherpass123", "CUSTOMER");

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
        }

        @Test
        @DisplayName("비밀번호가 8자보다 짧으면 400 (시나리오 #6)")
        void passwordTooShort() throws Exception {
            // when
            ResultActions result = signup("owner1", "abc", "OWNER");

            // then
            expectInvalidField(result, "password");
        }

        @Test
        @DisplayName("비밀번호가 20자보다 길면 400")
        void passwordTooLong() throws Exception {
            // when
            ResultActions result = signup("owner1", "a".repeat(21), "OWNER");

            // then
            expectInvalidField(result, "password");
        }

        @Test
        @DisplayName("아이디가 4자보다 짧으면 400")
        void usernameTooShort() throws Exception {
            // when
            ResultActions result = signup("abc", "password123", "OWNER");

            // then
            expectInvalidField(result, "username");
        }

        @Test
        @DisplayName("아이디가 20자보다 길면 400")
        void usernameTooLong() throws Exception {
            // when
            ResultActions result = signup("a".repeat(21), "password123", "OWNER");

            // then
            expectInvalidField(result, "username");
        }

        @Test
        @DisplayName("역할이 없으면 400")
        void roleMissing() throws Exception {
            // when
            ResultActions result = mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username": "owner1", "password": "password123"}
                            """));

            // then
            expectInvalidField(result, "role");
        }

        @Test
        @DisplayName("CUSTOMER·OWNER가 아닌 역할이면 400")
        void roleInvalid() throws Exception {
            // when
            ResultActions result = signup("owner1", "password123", "ADMIN");

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    @Nested
    @DisplayName("로그인 POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("아이디와 비밀번호가 맞으면 200과 Bearer 토큰을 응답한다 (시나리오 #7)")
        void success() throws Exception {
            // given
            signup("owner1", "password123", "OWNER");

            // when
            ResultActions result = login("owner1", "password123");

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(not(emptyString())))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600));
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401 INVALID_CREDENTIALS (시나리오 #8)")
        void wrongPassword() throws Exception {
            // given
            signup("owner1", "password123", "OWNER");

            // when
            ResultActions result = login("owner1", "wrongpass123");

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("없는 아이디면 401 INVALID_CREDENTIALS")
        void userNotFound() throws Exception {
            // when
            ResultActions result = login("nobody", "password123");

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("비밀번호가 없으면 400")
        void passwordMissing() throws Exception {
            // when
            ResultActions result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username": "owner1"}
                            """));

            // then
            expectInvalidField(result, "password");
        }
    }

    private ResultActions signup(String username, String password, String role) throws Exception {
        return mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "password": "%s", "role": "%s"}
                        """.formatted(username, password, role)));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "password": "%s"}
                        """.formatted(username, password)));
    }
}
