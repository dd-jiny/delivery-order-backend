package com.example.delivery.payment.application;

import com.example.delivery.order.domain.Order;
import com.example.delivery.order.domain.OrderService;
import com.example.delivery.payment.application.dto.PaymentCommand;
import com.example.delivery.payment.application.dto.PaymentResponse;
import com.example.delivery.payment.domain.Payment;
import com.example.delivery.payment.domain.PaymentMethod;
import com.example.delivery.payment.domain.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 유스케이스 흐름을 조율한다. 본인 주문 가져오기(주문 도메인) → 결제하기(결제 도메인)를 한 트랜잭션으로 묶는다.
 */
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * 404 → 403은 OrderService, 409(ORDERED가 아님)는 Order가 던진다.
     * 동시 결제 충돌은 커밋 시점(이 메서드가 끝난 뒤) 낙관적 락 예외로 나고 GlobalExceptionHandler가 409로 바꾼다.
     * 결제 수단 문자열은 PaymentRequest의 @Pattern으로 검증된 값이라 enum 변환이 실패하지 않는다.
     */
    @Transactional
    public PaymentResponse pay(Long customerId, Long orderId, PaymentCommand command) {
        Order order = orderService.getCustomerOrder(customerId, orderId);
        Payment payment = paymentService.pay(order, PaymentMethod.valueOf(command.method()));
        return PaymentResponse.from(payment);
    }
}
