package com.example.delivery.payment.application;

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
import com.example.delivery.order.domain.OrderService;
import com.example.delivery.order.domain.OrderStatus;
import com.example.delivery.payment.application.dto.PaymentCommand;
import com.example.delivery.payment.application.dto.PaymentResponse;
import com.example.delivery.payment.domain.Payment;
import com.example.delivery.payment.domain.PaymentMethod;
import com.example.delivery.payment.domain.PaymentService;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 주문·결제 두 도메인 서비스를 엮는 결제 흐름을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    private static final Long CUSTOMER_ID = 2L;
    private static final Long OTHER_CUSTOMER_ID = 3L;
    private static final Long ORDER_ID = 100L;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentFacade paymentFacade;

    private final PaymentCommand card = new PaymentCommand("CARD");

    @Test
    @DisplayName("본인 주문을 카드로 결제하고 결제 금액과 결제 후 주문 상태를 응답한다")
    void success() {
        // given
        User owner = UserFixture.withId(UserFixture.owner(), 1L);
        User customer = UserFixture.withId(UserFixture.customer(), CUSTOMER_ID);
        Menu kimbap = MenuFixture.withId(Menu.create(owner, "김밥", 3500L, null), 10L);
        Order order = OrderFixture.withId(Order.create(customer, kimbap, 2, "서울시 강남구 테헤란로 1"), ORDER_ID);
        Payment payment = Payment.complete(order, PaymentMethod.CARD);
        ReflectionTestUtils.setField(payment, "id", 1000L);
        OrderFixture.withStatus(order, OrderStatus.PAID);
        given(orderService.getCustomerOrder(CUSTOMER_ID, ORDER_ID)).willReturn(order);
        given(paymentService.pay(order, PaymentMethod.CARD)).willReturn(payment);

        // when
        PaymentResponse response = paymentFacade.pay(CUSTOMER_ID, ORDER_ID, card);

        // then
        assertThat(response.paymentId()).isEqualTo(1000L);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.amount()).isEqualTo(7000L);
        assertThat(response.method()).isEqualTo("CARD");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.orderStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("다른 손님의 주문이면 403 ORDER_ACCESS_DENIED로 거절하고 결제하지 않는다")
    void otherCustomer() {
        // given
        given(orderService.getCustomerOrder(OTHER_CUSTOMER_ID, ORDER_ID))
                .willThrow(new BusinessException(ErrorCode.ORDER_ACCESS_DENIED));

        // when & then
        assertThatThrownBy(() -> paymentFacade.pay(OTHER_CUSTOMER_ID, ORDER_ID, card))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
        verify(paymentService, never()).pay(any(), any());
    }
}
