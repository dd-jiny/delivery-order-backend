package com.example.delivery.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.user.application.dto.LoginCommand;
import com.example.delivery.user.application.dto.LoginResponse;
import com.example.delivery.user.application.dto.SignupCommand;
import com.example.delivery.user.application.dto.UserResponse;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.domain.UserRole;
import com.example.delivery.user.domain.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화·대조와 토큰 발급을 도메인 서비스와 엮는 흐름을 검증한다.
 * 토큰 발급은 인터페이스(TokenProvider)만 Mock으로 둔다 — Facade가 JWT 구현을 모르는지도 함께 확인된다.
 */
@ExtendWith(MockitoExtension.class)
class UserFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private UserFacade userFacade;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("비밀번호를 암호화해 등록하고 회원 정보를 응답한다")
        void success() {
            // given
            given(passwordEncoder.encode("password123")).willReturn("$2a$10$encoded");
            given(userService.register("owner1", "$2a$10$encoded", UserRole.OWNER))
                    .willReturn(UserFixture.withId(User.create("owner1", "$2a$10$encoded", UserRole.OWNER), 1L));

            // when
            UserResponse response = userFacade.signup(new SignupCommand("owner1", "password123", "OWNER"));

            // then
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("owner1");
            assertThat(response.role()).isEqualTo("OWNER");
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        private final LoginCommand command = new LoginCommand("owner1", "password123");

        @Test
        @DisplayName("비밀번호가 맞으면 Bearer 토큰과 만료 시간(초)을 응답한다")
        void success() {
            // given
            User owner = UserFixture.withId(UserFixture.owner(), 1L);
            given(userService.getLoginUser("owner1")).willReturn(owner);
            given(passwordEncoder.matches("password123", owner.getPassword())).willReturn(true);
            given(tokenProvider.createToken(1L, "owner1", UserRole.OWNER)).willReturn("issued-token");
            given(tokenProvider.getExpirationSeconds()).willReturn(3600L);

            // when
            LoginResponse response = userFacade.login(command);

            // then
            assertThat(response).isEqualTo(new LoginResponse("issued-token", "Bearer", 3600L));
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401 INVALID_CREDENTIALS로 거절하고 토큰을 발급하지 않는다")
        void wrongPassword() {
            // given
            User owner = UserFixture.withId(UserFixture.owner(), 1L);
            given(userService.getLoginUser("owner1")).willReturn(owner);
            given(passwordEncoder.matches("password123", owner.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userFacade.login(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            verify(tokenProvider, never()).createToken(any(), any(), any());
        }
    }
}
