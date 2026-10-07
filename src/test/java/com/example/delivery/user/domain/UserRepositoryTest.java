package com.example.delivery.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.support.RepositoryTestSupport;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class UserRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 아이디의 회원이 있으면 existsByUsername이 true다")
    void existsByUsername_exists() {
        // given
        userRepository.save(UserFixture.owner());

        // when & then
        assertThat(userRepository.existsByUsername("owner1")).isTrue();
    }

    @Test
    @DisplayName("같은 아이디의 회원이 없으면 existsByUsername이 false다")
    void existsByUsername_notExists() {
        // given
        userRepository.save(UserFixture.owner());

        // when & then
        assertThat(userRepository.existsByUsername("owner2")).isFalse();
    }

    @Test
    @DisplayName("탈퇴한 회원의 아이디도 existsByUsername이 true다")
    void existsByUsername_withdrawn() {
        // given
        userRepository.save(UserFixture.withdrawn(UserFixture.owner()));

        // when & then
        assertThat(userRepository.existsByUsername("owner1")).isTrue();
    }

    @Test
    @DisplayName("아이디로 회원을 찾는다")
    void findByUsername() {
        // given
        userRepository.save(UserFixture.owner());

        // when
        Optional<User> found = userRepository.findByUsername("owner1");

        // then
        assertThat(found).get().extracting(User::getUsername).isEqualTo("owner1");
    }

    @Test
    @DisplayName("없는 아이디로 찾으면 빈 값이다")
    void findByUsername_notFound() {
        // when
        Optional<User> found = userRepository.findByUsername("nobody");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("같은 아이디로 두 번 저장하면 DB UNIQUE 제약이 막는다")
    void uniqueUsername() {
        // given
        userRepository.saveAndFlush(UserFixture.owner());

        // when & then
        assertThatThrownBy(() -> userRepository.saveAndFlush(UserFixture.owner()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("역할과 상태는 DB에 문자열로 저장된다")
    void enumsStoredAsString() {
        // given
        User saved = userRepository.saveAndFlush(UserFixture.owner());

        // when
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT role, status FROM users WHERE id = ?", saved.getId());

        // then
        assertThat(row).containsEntry("role", "OWNER").containsEntry("status", "ACTIVE");
    }

    @Test
    @DisplayName("저장하면 생성·수정 시각이 채워진다")
    void auditing() {
        // when
        User saved = userRepository.saveAndFlush(UserFixture.owner());

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
