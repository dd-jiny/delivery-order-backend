package com.example.delivery.payment.domain;

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
import com.example.delivery.order.domain.Order;
import com.example.delivery.order.domain.OrderFixture;
import com.example.delivery.order.domain.OrderStatus;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;

    @BeforeEach
    void setUp() {
        User owner = UserFixture.withId(UserFixture.owner(), 1L);
        User customer = UserFixture.withId(UserFixture.customer(), 2L);
        Menu kimbap = MenuFixture.withId(Menu.create(owner, "김밥", 3500L, null), 10L);
        order = OrderFixture.withId(Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1"), 100L);
    }

    @Test
    @DisplayName("결제하면 주문이 PAID가 되고 주문 총액으로 완료된 결제 기록을 저장한다")
    void success() {
        // given
        given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Payment payment = paymentService.pay(order, PaymentMethod.CARD);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payment.getOrder()).isSameAs(order);
        assertThat(payment.getAmount()).isEqualTo(7000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("이미 결제된 주문이면 409 INVALID_ORDER_STATUS로 거절하고 결제 기록을 저장하지 않는다")
    void alreadyPaid() {
        // given
        OrderFixture.withStatus(order, OrderStatus.PAID);

        // when & then
        assertThatThrownBy(() -> paymentService.pay(order, PaymentMethod.CARD))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("취소된 주문이면 409 INVALID_ORDER_STATUS로 거절하고 결제 기록을 저장하지 않는다")
    void canceled() {
        // given
        OrderFixture.withStatus(order, OrderStatus.CANCELED);

        // when & then
        assertThatThrownBy(() -> paymentService.pay(order, PaymentMethod.CARD))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        verify(paymentRepository, never()).save(any());
    }
}
