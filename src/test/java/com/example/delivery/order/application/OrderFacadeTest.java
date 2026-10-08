package com.example.delivery.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.menu.domain.MenuService;
import com.example.delivery.order.application.dto.OrderCreateCommand;
import com.example.delivery.order.application.dto.OrderResponse;
import com.example.delivery.order.domain.Order;
import com.example.delivery.order.domain.OrderFixture;
import com.example.delivery.order.domain.OrderService;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.domain.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 메뉴·회원·주문 세 도메인 서비스를 엮는 주문 생성 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    private static final Long CUSTOMER_ID = 2L;
    private static final Long MENU_ID = 10L;

    @Mock
    private MenuService menuService;

    @Mock
    private UserService userService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderFacade orderFacade;

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        private final OrderCreateCommand command = new OrderCreateCommand(MENU_ID, 2, "서울시 강남구 테헤란로 1");

        @Test
        @DisplayName("조회한 메뉴와 토큰의 회원으로 주문을 만들고 주문 응답을 돌려준다")
        void success() {
            // given
            User owner = UserFixture.withId(UserFixture.owner(), 1L);
            User customer = UserFixture.withId(UserFixture.customer(), CUSTOMER_ID);
            Menu kimbap = MenuFixture.withId(Menu.create(owner, "김밥", 3500L, null), MENU_ID);
            Order order = OrderFixture.withId(Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1"), 100L);
            given(menuService.getMenu(MENU_ID)).willReturn(kimbap);
            given(userService.getReference(CUSTOMER_ID)).willReturn(customer);
            given(orderService.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1")).willReturn(order);

            // when
            OrderResponse response = orderFacade.createOrder(CUSTOMER_ID, command);

            // then
            assertThat(response.orderId()).isEqualTo(100L);
            assertThat(response.customerUsername()).isEqualTo("cust1");
            assertThat(response.menuId()).isEqualTo(MENU_ID);
            assertThat(response.menuName()).isEqualTo("김밥");
            assertThat(response.unitPrice()).isEqualTo(3500L);
            assertThat(response.quantity()).isEqualTo(2);
            assertThat(response.totalPrice()).isEqualTo(7000L);
            assertThat(response.deliveryAddress()).isEqualTo("서울시 강남구 테헤란로 1");
            assertThat(response.status()).isEqualTo("ORDERED");
        }

        @Test
        @DisplayName("없거나 삭제된 메뉴면 404 MENU_NOT_FOUND로 거절하고 주문을 만들지 않는다")
        void menuNotFound() {
            // given
            given(menuService.getMenu(MENU_ID)).willThrow(new BusinessException(ErrorCode.MENU_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> orderFacade.createOrder(CUSTOMER_ID, command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_NOT_FOUND);
            verify(orderService, never()).create(any(), any(), anyInt(), any());
        }
    }
}
