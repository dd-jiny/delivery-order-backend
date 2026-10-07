package com.example.delivery.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("회원을 생성하면 아이디·암호화된 비밀번호·역할이 저장된다")
    void create() {
        // given
        String encodedPassword = "$2a$10$encoded";

        // when
        User user = User.create("owner1", encodedPassword, UserRole.OWNER);

        // then
        assertThat(user.getUsername()).isEqualTo("owner1");
        assertThat(user.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(user.getRole()).isEqualTo(UserRole.OWNER);
    }

    @Test
    @DisplayName("새로 생성한 회원은 활동 상태이고 탈퇴 시각이 없다")
    void create_statusIsActive() {
        // when
        User user = User.create("cust1", "$2a$10$encoded", UserRole.CUSTOMER);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getWithdrawnAt()).isNull();
    }

    @Test
    @DisplayName("활동 상태인 회원은 isActive가 true다")
    void isActive_active() {
        // given
        User user = UserFixture.customer();

        // when & then
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("탈퇴한 회원은 isActive가 false다")
    void isActive_withdrawn() {
        // given
        User user = UserFixture.withdrawn(UserFixture.customer());

        // when & then
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("사장님 역할이면 isOwner가 true다")
    void isOwner_owner() {
        // given
        User user = UserFixture.owner();

        // when & then
        assertThat(user.isOwner()).isTrue();
    }

    @Test
    @DisplayName("손님 역할이면 isOwner가 false다")
    void isOwner_customer() {
        // given
        User user = UserFixture.customer();

        // when & then
        assertThat(user.isOwner()).isFalse();
    }
}
