package com.example.delivery.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MenuTest {

    private final User owner = UserFixture.withId(UserFixture.owner(), 1L);

    @Test
    @DisplayName("메뉴를 생성하면 주인·이름·가격·설명이 저장된다")
    void create() {
        // when
        Menu menu = Menu.create(owner, "김밥", 3000L, "기본 김밥");

        // then
        assertThat(menu.getOwner()).isSameAs(owner);
        assertThat(menu.getName()).isEqualTo("김밥");
        assertThat(menu.getPrice()).isEqualTo(3000L);
        assertThat(menu.getDescription()).isEqualTo("기본 김밥");
    }

    @Test
    @DisplayName("새로 생성한 메뉴는 판매 중이고 삭제되지 않은 상태다")
    void create_onSaleAndNotDeleted() {
        // when
        Menu menu = Menu.create(owner, "김밥", 3000L, null);

        // then
        assertThat(menu.getStatus()).isEqualTo(MenuStatus.ON_SALE);
        assertThat(menu.getDeletedAt()).isNull();
        assertThat(menu.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("수정하면 이름·가격·설명을 모두 교체한다")
    void update() {
        // given
        Menu menu = MenuFixture.kimbap(owner);

        // when
        menu.update("참치김밥", 3500L, "참치가 들어간 김밥");

        // then
        assertThat(menu.getName()).isEqualTo("참치김밥");
        assertThat(menu.getPrice()).isEqualTo(3500L);
        assertThat(menu.getDescription()).isEqualTo("참치가 들어간 김밥");
    }

    @Test
    @DisplayName("설명 없이 수정하면 설명이 비워진다")
    void update_withoutDescription() {
        // given
        Menu menu = MenuFixture.kimbap(owner);

        // when
        menu.update("김밥", 3500L, null);

        // then
        assertThat(menu.getDescription()).isNull();
    }

    @Test
    @DisplayName("삭제하면 받은 시각이 삭제 시각으로 기록되고 삭제 상태가 된다")
    void delete() {
        // given
        Menu menu = MenuFixture.kimbap(owner);

        // when
        menu.delete(LocalDateTime.of(2026, 10, 7, 14, 30));

        // then
        assertThat(menu.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 7, 14, 30));
        assertThat(menu.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("메뉴 주인의 회원 ID면 isOwnedBy가 true다")
    void isOwnedBy_owner() {
        // given
        Menu menu = MenuFixture.kimbap(owner);

        // when & then
        assertThat(menu.isOwnedBy(1L)).isTrue();
    }

    @Test
    @DisplayName("다른 회원 ID면 isOwnedBy가 false다")
    void isOwnedBy_other() {
        // given
        Menu menu = MenuFixture.kimbap(owner);

        // when & then
        assertThat(menu.isOwnedBy(2L)).isFalse();
    }

    @Test
    @DisplayName("판매 중인 메뉴는 isOnSale이 true다")
    void isOnSale_onSale() {
        // given
        Menu menu = MenuFixture.kimbap(owner);

        // when & then
        assertThat(menu.isOnSale()).isTrue();
    }

    @Test
    @DisplayName("품절 메뉴는 isOnSale이 false다")
    void isOnSale_soldOut() {
        // given
        Menu menu = MenuFixture.soldOut(MenuFixture.kimbap(owner));

        // when & then
        assertThat(menu.isOnSale()).isFalse();
    }
}
