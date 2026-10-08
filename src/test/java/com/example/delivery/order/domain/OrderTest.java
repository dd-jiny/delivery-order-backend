package com.example.delivery.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class OrderTest {

    private final User owner = UserFixture.withId(UserFixture.owner(), 1L);
    private final User customer = UserFixture.withId(UserFixture.customer(), 2L);
    private final Menu kimbap = MenuFixture.withId(Menu.create(owner, "김밥", 3500L, null), 10L);

    @Nested
    @DisplayName("주문 생성")
    class Create {

        @Test
        @DisplayName("메뉴 이름과 가격을 주문에 복사하고 총액은 가격 × 수량이다")
        void snapshotAndTotalPrice() {
            // when
            Order order = Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1");

            // then
            assertThat(order.getMenuName()).isEqualTo("김밥");
            assertThat(order.getUnitPrice()).isEqualTo(3500L);
            assertThat(order.getQuantity()).isEqualTo(2);
            assertThat(order.getTotalPrice()).isEqualTo(7000L);
        }

        @Test
        @DisplayName("주문자·메뉴·배송 주소를 담고 처음 상태는 ORDERED다")
        void initialState() {
            // when
            Order order = Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1");

            // then
            assertThat(order.getCustomer()).isSameAs(customer);
            assertThat(order.getMenu()).isSameAs(kimbap);
            assertThat(order.getDeliveryAddress()).isEqualTo("서울시 강남구 테헤란로 1");
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        }

        @Test
        @DisplayName("주문 후 메뉴가 수정되어도 주문의 메뉴 이름과 가격은 그대로다")
        void snapshotKeptAfterMenuUpdate() {
            // given
            Order order = Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1");

            // when
            kimbap.update("참치김밥", 4500L, null);

            // then
            assertThat(order.getMenuName()).isEqualTo("김밥");
            assertThat(order.getUnitPrice()).isEqualTo(3500L);
            assertThat(order.getTotalPrice()).isEqualTo(7000L);
        }

        @Test
        @DisplayName("품절 메뉴는 주문할 수 없다 (409 MENU_SOLD_OUT)")
        void soldOut() {
            // given
            Menu soldOut = MenuFixture.soldOut(kimbap);

            // when & then
            assertThatThrownBy(() -> Order.create(customer, soldOut, 2, "서울시 강남구 테헤란로 1"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_SOLD_OUT);
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("주문요청 상태의 주문을 결제하면 PAID가 된다")
        void pay() {
            // given
            Order order = orderIn(OrderStatus.ORDERED);

            // when
            order.pay();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("주문요청 상태의 주문을 취소하면 CANCELED가 된다")
        void cancel() {
            // given
            Order order = orderIn(OrderStatus.ORDERED);

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("결제완료 상태의 주문을 수락하면 ACCEPTED가 된다")
        void accept() {
            // given
            Order order = orderIn(OrderStatus.PAID);

            // when
            order.accept();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        @DisplayName("수락 상태의 주문을 배달완료하면 COMPLETED가 된다")
        void complete() {
            // given
            Order order = orderIn(OrderStatus.ACCEPTED);

            // when
            order.complete();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, mode = Mode.EXCLUDE, names = "ORDERED")
        @DisplayName("주문요청이 아닌 주문은 결제할 수 없다 (409)")
        void pay_invalidStatus(OrderStatus status) {
            // given
            Order order = orderIn(status);

            // when & then
            assertThatThrownBy(order::pay)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, mode = Mode.EXCLUDE, names = "ORDERED")
        @DisplayName("주문요청이 아닌 주문은 취소할 수 없다 (409)")
        void cancel_invalidStatus(OrderStatus status) {
            // given
            Order order = orderIn(status);

            // when & then
            assertThatThrownBy(order::cancel)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, mode = Mode.EXCLUDE, names = "PAID")
        @DisplayName("결제완료가 아닌 주문은 수락할 수 없다 (409)")
        void accept_invalidStatus(OrderStatus status) {
            // given
            Order order = orderIn(status);

            // when & then
            assertThatThrownBy(order::accept)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = OrderStatus.class, mode = Mode.EXCLUDE, names = "ACCEPTED")
        @DisplayName("수락되지 않은 주문은 배달완료할 수 없다 (409)")
        void complete_invalidStatus(OrderStatus status) {
            // given
            Order order = orderIn(status);

            // when & then
            assertThatThrownBy(order::complete)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        }

        @Test
        @DisplayName("거절된 상태 변경은 주문 상태를 바꾸지 않는다")
        void rejectedKeepsStatus() {
            // given
            Order order = orderIn(OrderStatus.PAID);

            // when
            assertThatThrownBy(order::cancel).isInstanceOf(BusinessException.class);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        private Order orderIn(OrderStatus status) {
            return OrderFixture.withStatus(OrderFixture.order(customer, kimbap), status);
        }
    }

    @Nested
    @DisplayName("소유 확인")
    class Ownership {

        @Test
        @DisplayName("주문자의 회원 ID면 isOrderedBy가 true다")
        void isOrderedBy_customer() {
            // given
            Order order = OrderFixture.order(customer, kimbap);

            // when & then
            assertThat(order.isOrderedBy(2L)).isTrue();
        }

        @Test
        @DisplayName("다른 회원 ID면 isOrderedBy가 false다")
        void isOrderedBy_other() {
            // given
            Order order = OrderFixture.order(customer, kimbap);

            // when & then
            assertThat(order.isOrderedBy(3L)).isFalse();
        }

        @Test
        @DisplayName("주문한 메뉴 주인의 회원 ID면 isMenuOwnedBy가 true다")
        void isMenuOwnedBy_owner() {
            // given
            Order order = OrderFixture.order(customer, kimbap);

            // when & then
            assertThat(order.isMenuOwnedBy(1L)).isTrue();
        }

        @Test
        @DisplayName("다른 사장님의 회원 ID면 isMenuOwnedBy가 false다")
        void isMenuOwnedBy_other() {
            // given
            Order order = OrderFixture.order(customer, kimbap);

            // when & then
            assertThat(order.isMenuOwnedBy(3L)).isFalse();
        }
    }
}
