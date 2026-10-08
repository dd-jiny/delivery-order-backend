package com.example.delivery.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long CUSTOMER_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;
    private static final Long ORDER_ID = 100L;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private Menu kimbap;

    @BeforeEach
    void setUp() {
        User owner = UserFixture.withId(UserFixture.owner(), OWNER_ID);
        customer = UserFixture.withId(UserFixture.customer(), CUSTOMER_ID);
        kimbap = MenuFixture.withId(Menu.create(owner, "김밥", 3500L, null), 10L);
    }

    private Order savedOrder(OrderStatus status) {
        Order order = OrderFixture.withStatus(OrderFixture.withId(OrderFixture.order(customer, kimbap), ORDER_ID), status);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        return order;
    }

    private void noOrder() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());
    }

    @Nested
    @DisplayName("주문 생성")
    class Create {

        @Test
        @DisplayName("주문을 만들어 저장한다")
        void success() {
            // given
            given(orderRepository.save(any(Order.class)))
                    .willAnswer(invocation -> OrderFixture.withId(invocation.getArgument(0), ORDER_ID));

            // when
            Order order = orderService.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1");

            // then
            assertThat(order.getId()).isEqualTo(ORDER_ID);
            assertThat(order.getTotalPrice()).isEqualTo(7000L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        }

        @Test
        @DisplayName("품절 메뉴면 409 MENU_SOLD_OUT으로 거절하고 저장하지 않는다")
        void soldOut() {
            // given
            Menu soldOut = MenuFixture.soldOut(kimbap);

            // when & then
            assertThatThrownBy(() -> orderService.create(customer, soldOut, 2, "서울시 강남구 테헤란로 1"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_SOLD_OUT);
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("주문 목록")
    class GetOrders {

        @Test
        @DisplayName("손님은 자신이 한 주문 목록을 받는다")
        void customer() {
            // given
            Order order = OrderFixture.order(customer, kimbap);
            given(orderRepository.findAllByCustomerId(CUSTOMER_ID)).willReturn(List.of(order));

            // when
            List<Order> orders = orderService.getOrders(CUSTOMER_ID, UserRole.CUSTOMER);

            // then
            assertThat(orders).containsExactly(order);
        }

        @Test
        @DisplayName("사장님은 자신의 메뉴에 들어온 주문 목록을 받는다")
        void owner() {
            // given
            Order order = OrderFixture.order(customer, kimbap);
            given(orderRepository.findAllByMenuOwnerId(OWNER_ID)).willReturn(List.of(order));

            // when
            List<Order> orders = orderService.getOrders(OWNER_ID, UserRole.OWNER);

            // then
            assertThat(orders).containsExactly(order);
        }
    }

    @Nested
    @DisplayName("손님의 주문 조회")
    class GetCustomerOrder {

        @Test
        @DisplayName("본인 주문을 조회한다")
        void success() {
            // given
            Order order = savedOrder(OrderStatus.ORDERED);

            // when
            Order found = orderService.getCustomerOrder(CUSTOMER_ID, ORDER_ID);

            // then
            assertThat(found).isSameAs(order);
        }

        @Test
        @DisplayName("없는 주문이면 404 ORDER_NOT_FOUND")
        void notFound() {
            // given
            noOrder();

            // when & then
            assertThatThrownBy(() -> orderService.getCustomerOrder(CUSTOMER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 손님의 주문이면 403 ORDER_ACCESS_DENIED")
        void otherCustomer() {
            // given
            savedOrder(OrderStatus.ORDERED);

            // when & then
            assertThatThrownBy(() -> orderService.getCustomerOrder(OTHER_USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("사장님의 주문 조회")
    class GetOwnerOrder {

        @Test
        @DisplayName("본인 메뉴의 주문을 조회한다")
        void success() {
            // given
            Order order = savedOrder(OrderStatus.PAID);

            // when
            Order found = orderService.getOwnerOrder(OWNER_ID, ORDER_ID);

            // then
            assertThat(found).isSameAs(order);
        }

        @Test
        @DisplayName("없는 주문이면 404 ORDER_NOT_FOUND")
        void notFound() {
            // given
            noOrder();

            // when & then
            assertThatThrownBy(() -> orderService.getOwnerOrder(OWNER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사장님 메뉴의 주문이면 403 ORDER_ACCESS_DENIED")
        void otherOwner() {
            // given
            savedOrder(OrderStatus.PAID);

            // when & then
            assertThatThrownBy(() -> orderService.getOwnerOrder(OTHER_USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class Cancel {

        @Test
        @DisplayName("본인의 주문요청 상태 주문을 취소한다")
        void success() {
            // given
            savedOrder(OrderStatus.ORDERED);

            // when
            Order order = orderService.cancel(CUSTOMER_ID, ORDER_ID);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("없는 주문이면 404 ORDER_NOT_FOUND")
        void notFound() {
            // given
            noOrder();

            // when & then
            assertThatThrownBy(() -> orderService.cancel(CUSTOMER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 손님의 주문이면 403 ORDER_ACCESS_DENIED로 거절하고 취소하지 않는다")
        void otherCustomer() {
            // given
            Order order = savedOrder(OrderStatus.ORDERED);

            // when & then
            assertThatThrownBy(() -> orderService.cancel(OTHER_USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        }

        @Test
        @DisplayName("결제된 주문이면 409 INVALID_ORDER_STATUS")
        void paid() {
            // given
            savedOrder(OrderStatus.PAID);

            // when & then
            assertThatThrownBy(() -> orderService.cancel(CUSTOMER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        }

        @Test
        @DisplayName("다른 손님의 결제된 주문이면 상태보다 소유를 먼저 확인해 403이다")
        void otherCustomerAndPaid() {
            // given
            savedOrder(OrderStatus.PAID);

            // when & then
            assertThatThrownBy(() -> orderService.cancel(OTHER_USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("주문 수락")
    class Accept {

        @Test
        @DisplayName("본인 메뉴의 결제완료 주문을 수락한다")
        void success() {
            // given
            savedOrder(OrderStatus.PAID);

            // when
            Order order = orderService.accept(OWNER_ID, ORDER_ID);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        @DisplayName("없는 주문이면 404 ORDER_NOT_FOUND")
        void notFound() {
            // given
            noOrder();

            // when & then
            assertThatThrownBy(() -> orderService.accept(OWNER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사장님 메뉴의 주문이면 403 ORDER_ACCESS_DENIED로 거절하고 수락하지 않는다")
        void otherOwner() {
            // given
            Order order = savedOrder(OrderStatus.PAID);

            // when & then
            assertThatThrownBy(() -> orderService.accept(OTHER_USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("결제 전 주문이면 409 INVALID_ORDER_STATUS")
        void notPaid() {
            // given
            savedOrder(OrderStatus.ORDERED);

            // when & then
            assertThatThrownBy(() -> orderService.accept(OWNER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        }

        @Test
        @DisplayName("다른 사장님 메뉴의 결제 전 주문이면 상태보다 소유를 먼저 확인해 403이다")
        void otherOwnerAndNotPaid() {
            // given
            savedOrder(OrderStatus.ORDERED);

            // when & then
            assertThatThrownBy(() -> orderService.accept(OTHER_USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("주문 배달완료")
    class Complete {

        @Test
        @DisplayName("본인 메뉴의 수락된 주문을 배달완료한다")
        void success() {
            // given
            savedOrder(OrderStatus.ACCEPTED);

            // when
            Order order = orderService.complete(OWNER_ID, ORDER_ID);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("없는 주문이면 404 ORDER_NOT_FOUND")
        void notFound() {
            // given
            noOrder();

            // when & then
            assertThatThrownBy(() -> orderService.complete(OWNER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사장님 메뉴의 주문이면 403 ORDER_ACCESS_DENIED")
        void otherOwner() {
            // given
            savedOrder(OrderStatus.ACCEPTED);

            // when & then
            assertThatThrownBy(() -> orderService.complete(OTHER_USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
        }

        @Test
        @DisplayName("수락되지 않은 주문이면 409 INVALID_ORDER_STATUS")
        void notAccepted() {
            // given
            savedOrder(OrderStatus.PAID);

            // when & then
            assertThatThrownBy(() -> orderService.complete(OWNER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        }
    }
}
