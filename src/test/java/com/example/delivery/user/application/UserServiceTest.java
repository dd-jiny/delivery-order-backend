package com.example.delivery.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.JwtProvider;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.domain.UserRepository;
import com.example.delivery.user.domain.UserRole;
import com.example.delivery.user.presentation.LoginRequest;
import com.example.delivery.user.presentation.LoginResponse;
import com.example.delivery.user.presentation.SignupRequest;
import com.example.delivery.user.presentation.UserResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        private final SignupRequest request = new SignupRequest("owner1", "password123", UserRole.OWNER);

        @Test
        @DisplayName("가입하면 회원 PK·아이디·역할을 응답한다")
        void success() {
            // given
            given(userRepository.existsByUsername("owner1")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("$2a$10$encoded");
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> UserFixture.withId(invocation.getArgument(0), 1L));

            // when
            UserResponse response = userService.signup(request);

            // then
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("owner1");
            assertThat(response.role()).isEqualTo(UserRole.OWNER);
        }

        @Test
        @DisplayName("비밀번호를 암호화해 활동 상태로 저장한다")
        void savesEncodedPassword() {
            // given
            given(userRepository.existsByUsername("owner1")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("$2a$10$encoded");
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> UserFixture.withId(invocation.getArgument(0), 1L));

            // when
            userService.signup(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(captor.getValue().isActive()).isTrue();
        }

        @Test
        @DisplayName("이미 있는 아이디면 409 DUPLICATE_USERNAME으로 거절하고 저장하지 않는다")
        void duplicateUsername() {
            // given
            given(userRepository.existsByUsername("owner1")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("동시 가입으로 DB UNIQUE 제약에 걸리면 409 DUPLICATE_USERNAME으로 거절한다")
        void duplicateUsername_concurrentSignup() {
            // given
            given(userRepository.existsByUsername("owner1")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("$2a$10$encoded");
            given(userRepository.save(any(User.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        private final LoginRequest request = new LoginRequest("owner1", "password123");

        @Test
        @DisplayName("아이디와 비밀번호가 맞으면 Bearer 토큰과 만료 시간(초)을 응답한다")
        void success() {
            // given
            User owner = UserFixture.withId(UserFixture.owner(), 1L);
            given(userRepository.findByUsername("owner1")).willReturn(Optional.of(owner));
            given(passwordEncoder.matches("password123", owner.getPassword())).willReturn(true);
            given(jwtProvider.createToken(1L, "owner1", UserRole.OWNER)).willReturn("issued-token");
            given(jwtProvider.getExpirationSeconds()).willReturn(3600L);

            // when
            LoginResponse response = userService.login(request);

            // then
            assertThat(response).isEqualTo(new LoginResponse("issued-token", "Bearer", 3600L));
        }

        @Test
        @DisplayName("없는 아이디면 401 INVALID_CREDENTIALS로 거절한다")
        void userNotFound() {
            // given
            given(userRepository.findByUsername("owner1")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401 INVALID_CREDENTIALS로 거절한다")
        void wrongPassword() {
            // given
            User owner = UserFixture.withId(UserFixture.owner(), 1L);
            given(userRepository.findByUsername("owner1")).willReturn(Optional.of(owner));
            given(passwordEncoder.matches("password123", owner.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("탈퇴한 회원은 401 INVALID_CREDENTIALS로 거절하고 토큰을 발급하지 않는다")
        void withdrawnUser() {
            // given
            User withdrawn = UserFixture.withdrawn(UserFixture.withId(UserFixture.owner(), 1L));
            given(userRepository.findByUsername("owner1")).willReturn(Optional.of(withdrawn));

            // when & then
            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            verify(jwtProvider, never()).createToken(any(), any(), any());
        }
    }
}
