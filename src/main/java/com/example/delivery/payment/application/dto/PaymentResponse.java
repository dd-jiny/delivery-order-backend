package com.example.delivery.payment.application.dto;

import com.example.delivery.payment.domain.Payment;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 결제 응답. 결제 후 주문 상태(orderStatus)를 함께 담는다. domain enum은 문자열로 바꾼다.
 */
public record PaymentResponse(
        Long paymentId,
        Long orderId,
        Long amount,
        String method,
        String status,
        String orderStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    /** 트랜잭션 안에서 호출한다. orderStatus는 결제로 바뀐 뒤의 주문 상태(PAID)다. */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getOrder().getStatus().name(),
                payment.getCreatedAt());
    }
}
