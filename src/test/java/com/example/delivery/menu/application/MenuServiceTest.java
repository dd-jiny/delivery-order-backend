package com.example.delivery.menu.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.global.presentation.PageResponse;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.menu.domain.MenuRepository;
import com.example.delivery.menu.domain.MenuStatus;
import com.example.delivery.menu.presentation.MenuRequest;
import com.example.delivery.menu.presentation.MenuResponse;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Long MENU_ID = 10L;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private UserRepository userRepository;

    private MenuService menuService;

    private User owner;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-07T05:30:00Z"), ZoneId.of("Asia/Seoul"));
        menuService = new MenuService(menuRepository, userRepository, fixedClock);
        owner = UserFixture.withId(UserFixture.owner(), OWNER_ID);
    }

    @Nested
    @DisplayName("메뉴 등록")
    class Create {

        @Test
        @DisplayName("토큰의 회원을 주인으로 판매 중인 메뉴를 등록한다")
        void success() {
            // given
            given(userRepository.getReferenceById(OWNER_ID)).willReturn(owner);
            given(menuRepository.save(any(Menu.class)))
                    .willAnswer(invocation -> MenuFixture.withId(invocation.getArgument(0), MENU_ID));

            // when
            MenuResponse response = menuService.create(OWNER_ID, new MenuRequest("김밥", 3000L, "기본 김밥"));

            // then
            assertThat(response.menuId()).isEqualTo(MENU_ID);
            assertThat(response.ownerId()).isEqualTo(OWNER_ID);
            assertThat(response.name()).isEqualTo("김밥");
            assertThat(response.price()).isEqualTo(3000L);
            assertThat(response.description()).isEqualTo("기본 김밥");
            assertThat(response.status()).isEqualTo(MenuStatus.ON_SALE);
        }
    }

    @Nested
    @DisplayName("메뉴 목록 조회")
    class GetMenus {

        @Test
        @DisplayName("삭제되지 않은 메뉴를 최신 등록순으로 조회해 페이지 응답으로 돌려준다")
        void success() {
            // given
            PageRequest latestFirst = PageRequest.of(0, 10,
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
            Menu menu = MenuFixture.withId(MenuFixture.kimbap(owner), MENU_ID);
            given(menuRepository.findAllByDeletedAtIsNull(latestFirst))
                    .willReturn(new PageImpl<>(List.of(menu), latestFirst, 1));

            // when
            PageResponse<MenuResponse> response = menuService.getMenus(0, 10);

            // then
            assertThat(response.content()).extracting(MenuResponse::menuId).containsExactly(MENU_ID);
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("메뉴 단건 조회")
    class GetMenu {

        @Test
        @DisplayName("메뉴를 조회한다")
        void success() {
            // given
            Menu menu = MenuFixture.withId(MenuFixture.kimbap(owner), MENU_ID);
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.of(menu));

            // when
            MenuResponse response = menuService.getMenu(MENU_ID);

            // then
            assertThat(response.menuId()).isEqualTo(MENU_ID);
            assertThat(response.name()).isEqualTo("김밥");
        }

        @Test
        @DisplayName("없거나 삭제된 메뉴면 404 MENU_NOT_FOUND")
        void notFound() {
            // given
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> menuService.getMenu(MENU_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("메뉴 수정")
    class Update {

        private final MenuRequest request = new MenuRequest("김밥", 3500L, "가격 인상");

        @Test
        @DisplayName("본인 메뉴의 이름·가격·설명을 수정한다")
        void success() {
            // given
            Menu menu = MenuFixture.withId(MenuFixture.kimbap(owner), MENU_ID);
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.of(menu));

            // when
            MenuResponse response = menuService.update(OWNER_ID, MENU_ID, request);

            // then
            assertThat(response.price()).isEqualTo(3500L);
            assertThat(response.description()).isEqualTo("가격 인상");
        }

        @Test
        @DisplayName("없거나 삭제된 메뉴면 404 MENU_NOT_FOUND")
        void notFound() {
            // given
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> menuService.update(OWNER_ID, MENU_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사장님의 메뉴면 403 MENU_ACCESS_DENIED로 거절하고 수정하지 않는다")
        void otherOwner() {
            // given
            Menu menu = MenuFixture.withId(MenuFixture.kimbap(owner), MENU_ID);
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.of(menu));

            // when & then
            assertThatThrownBy(() -> menuService.update(OTHER_OWNER_ID, MENU_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_ACCESS_DENIED);
            assertThat(menu.getPrice()).isEqualTo(3000L);
        }
    }

    @Nested
    @DisplayName("메뉴 삭제")
    class Delete {

        @Test
        @DisplayName("본인 메뉴를 Clock의 현재 시각으로 삭제 표시한다")
        void success() {
            // given
            Menu menu = MenuFixture.withId(MenuFixture.kimbap(owner), MENU_ID);
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.of(menu));

            // when
            menuService.delete(OWNER_ID, MENU_ID);

            // then
            assertThat(menu.isDeleted()).isTrue();
            assertThat(menu.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 7, 14, 30));
        }

        @Test
        @DisplayName("없거나 이미 삭제된 메뉴면 404 MENU_NOT_FOUND")
        void notFound() {
            // given
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> menuService.delete(OWNER_ID, MENU_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사장님의 메뉴면 403 MENU_ACCESS_DENIED로 거절하고 삭제하지 않는다")
        void otherOwner() {
            // given
            Menu menu = MenuFixture.withId(MenuFixture.kimbap(owner), MENU_ID);
            given(menuRepository.findByIdAndDeletedAtIsNull(MENU_ID)).willReturn(Optional.of(menu));

            // when & then
            assertThatThrownBy(() -> menuService.delete(OTHER_OWNER_ID, MENU_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_ACCESS_DENIED);
            assertThat(menu.isDeleted()).isFalse();
        }
    }
}
