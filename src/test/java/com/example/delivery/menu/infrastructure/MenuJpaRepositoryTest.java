package com.example.delivery.menu.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.support.RepositoryTestSupport;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.infrastructure.UserJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class MenuJpaRepositoryTest extends RepositoryTestSupport {

    private static final LocalDateTime DELETED_AT = LocalDateTime.of(2026, 10, 7, 14, 30);

    @Autowired
    private MenuJpaRepository menuRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(UserFixture.owner());
    }

    @Test
    @DisplayName("목록 조회는 삭제된 메뉴를 빼고 품절 메뉴는 포함한다")
    void findAllByDeletedAtIsNull_excludesDeleted() {
        // given
        menuRepository.save(MenuFixture.menu(owner, "김밥"));
        menuRepository.save(MenuFixture.soldOut(MenuFixture.menu(owner, "라면")));
        Menu deleted = MenuFixture.menu(owner, "우동");
        deleted.delete(DELETED_AT);
        menuRepository.save(deleted);

        // when
        Page<Menu> page = menuRepository.findAllByDeletedAtIsNull(PageRequest.of(0, 10));

        // then
        assertThat(page.getContent()).extracting(Menu::getName).containsExactlyInAnyOrder("김밥", "라면");
    }

    @Test
    @DisplayName("목록 조회는 요청한 정렬과 페이지 크기를 따른다")
    void findAllByDeletedAtIsNull_paging() {
        // given
        menuRepository.save(MenuFixture.menu(owner, "김밥"));
        menuRepository.save(MenuFixture.menu(owner, "라면"));
        menuRepository.save(MenuFixture.menu(owner, "우동"));

        // when
        Page<Menu> page = menuRepository.findAllByDeletedAtIsNull(
                PageRequest.of(0, 2, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

        // then
        assertThat(page.getContent()).extracting(Menu::getName).containsExactly("우동", "라면");
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("삭제되지 않은 메뉴는 ID로 찾는다")
    void findByIdAndDeletedAtIsNull() {
        // given
        Menu menu = menuRepository.save(MenuFixture.kimbap(owner));

        // when
        Optional<Menu> found = menuRepository.findByIdAndDeletedAtIsNull(menu.getId());

        // then
        assertThat(found).get().extracting(Menu::getName).isEqualTo("김밥");
    }

    @Test
    @DisplayName("삭제된 메뉴는 ID로 찾으면 빈 값이다")
    void findByIdAndDeletedAtIsNull_deleted() {
        // given
        Menu menu = MenuFixture.kimbap(owner);
        menu.delete(DELETED_AT);
        menuRepository.save(menu);

        // when
        Optional<Menu> found = menuRepository.findByIdAndDeletedAtIsNull(menu.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("삭제한 메뉴의 행은 남아 있고 deleted_at만 채워진다")
    void softDelete_rowRemains() {
        // given
        Menu menu = menuRepository.saveAndFlush(MenuFixture.kimbap(owner));

        // when
        menu.delete(DELETED_AT);
        entityManager.flush();

        // then
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT name, deleted_at FROM menus WHERE id = ?", menu.getId());
        assertThat(row).containsEntry("name", "김밥");
        assertThat(row.get("deleted_at")).isNotNull();
    }

    @Test
    @DisplayName("판매 상태는 DB에 문자열로 저장된다")
    void statusStoredAsString() {
        // given
        Menu menu = menuRepository.saveAndFlush(MenuFixture.kimbap(owner));

        // when
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM menus WHERE id = ?", String.class, menu.getId());

        // then
        assertThat(status).isEqualTo("ON_SALE");
    }

    @Test
    @DisplayName("메뉴를 수정하면 수정 시각이 갱신된다")
    void update_refreshesUpdatedAt() {
        // given
        Menu saved = menuRepository.saveAndFlush(MenuFixture.kimbap(owner));
        jdbcTemplate.update("UPDATE menus SET updated_at = '2020-01-01 00:00:00' WHERE id = ?", saved.getId());
        entityManager.clear();
        Menu menu = menuRepository.findById(saved.getId()).orElseThrow();

        // when
        menu.update("김밥", 3500L, null);
        entityManager.flush();

        // then
        assertThat(menu.getUpdatedAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
    }
}
