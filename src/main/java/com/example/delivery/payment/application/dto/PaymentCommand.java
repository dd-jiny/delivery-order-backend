package com.example.delivery.payment.application.dto;

/**
 * 결제 입력. 결제 수단은 presentation(PaymentRequest)의 @Pattern으로 검증된 문자열이다. 금액은 받지 않는다.
 */
public record PaymentCommand(String method) {
}
