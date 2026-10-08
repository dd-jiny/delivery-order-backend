package com.example.delivery.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.order.domain.Order;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private final User owner = UserFixture.withId(UserFixture.owner(), 1L);
    private final User customer = UserFixture.withId(UserFixture.customer(), 2L);
    private final Menu kimbap = MenuFixture.withId(Menu.create(owner, "김밥", 3500L, null), 10L);

    @Test
    @DisplayName("결제 금액은 주문 총액이다")
    void amountIsOrderTotalPrice() {
        // given
        Order order = Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1");

        // when
        Payment payment = Payment.complete(order, PaymentMethod.CARD);

        // then
        assertThat(payment.getAmount()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("결제한 주문과 수단을 담고 상태는 COMPLETED다")
    void completed() {
        // given
        Order order = Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1");

        // when
        Payment payment = Payment.complete(order, PaymentMethod.CARD);

        // then
        assertThat(payment.getOrder()).isSameAs(order);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }
}
