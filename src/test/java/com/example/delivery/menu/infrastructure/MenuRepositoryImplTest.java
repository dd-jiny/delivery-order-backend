package com.example.delivery.menu.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.menu.domain.MenuRepository;
import com.example.delivery.support.RepositoryTestSupport;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

/**
 * domain의 MenuRepository 약속(최신 등록순, 삭제 제외)을 구현이 지키는지 확인한다.
 */
@Import(MenuRepositoryImpl.class)
class MenuRepositoryImplTest extends RepositoryTestSupport {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userJpaRepository.save(UserFixture.owner());
    }

    @Test
    @DisplayName("목록은 최신 등록순으로 요청한 페이지 크기만큼 조회한다")
    void findAllExcludingDeleted_latestFirst() {
        // given
        menuRepository.save(MenuFixture.menu(owner, "김밥"));
        menuRepository.save(MenuFixture.menu(owner, "라면"));
        menuRepository.save(MenuFixture.menu(owner, "우동"));

        // when
        Page<Menu> page = menuRepository.findAllExcludingDeleted(0, 2);

        // then
        assertThat(page.getContent()).extracting(Menu::getName).containsExactly("우동", "라면");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
}
