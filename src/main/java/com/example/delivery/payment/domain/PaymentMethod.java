package com.example.delivery.payment.domain;

/**
 * 결제 수단. 이번 범위는 카드만. 값을 추가하면 PaymentRequest의 @Pattern도 함께 고친다 (04 D-32).
 */
public enum PaymentMethod {
    CARD
}
