package com.example.delivery.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("회원 등록")
    class Register {

        @Test
        @DisplayName("암호화된 비밀번호로 활동 상태의 회원을 저장한다")
        void success() {
            // given
            given(userRepository.existsByUsername("owner1")).willReturn(false);
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> UserFixture.withId(invocation.getArgument(0), 1L));

            // when
            User user = userService.register("owner1", "$2a$10$encoded", UserRole.OWNER);

            // then
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getUsername()).isEqualTo("owner1");
            assertThat(user.getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(user.getRole()).isEqualTo(UserRole.OWNER);
            assertThat(user.isActive()).isTrue();
        }

        @Test
        @DisplayName("이미 있는 아이디면 409 DUPLICATE_USERNAME으로 거절하고 저장하지 않는다")
        void duplicateUsername() {
            // given
            given(userRepository.existsByUsername("owner1")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.register("owner1", "$2a$10$encoded", UserRole.OWNER))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("로그인 대상 조회")
    class GetLoginUser {

        @Test
        @DisplayName("활동 중인 회원을 아이디로 찾는다")
        void success() {
            // given
            User owner = UserFixture.withId(UserFixture.owner(), 1L);
            given(userRepository.findByUsername("owner1")).willReturn(Optional.of(owner));

            // when
            User user = userService.getLoginUser("owner1");

            // then
            assertThat(user).isSameAs(owner);
        }

        @Test
        @DisplayName("없는 아이디면 401 INVALID_CREDENTIALS")
        void userNotFound() {
            // given
            given(userRepository.findByUsername("owner1")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getLoginUser("owner1"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("탈퇴한 회원이면 401 INVALID_CREDENTIALS")
        void withdrawnUser() {
            // given
            User withdrawn = UserFixture.withdrawn(UserFixture.withId(UserFixture.owner(), 1L));
            given(userRepository.findByUsername("owner1")).willReturn(Optional.of(withdrawn));

            // when & then
            assertThatThrownBy(() -> userService.getLoginUser("owner1"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
