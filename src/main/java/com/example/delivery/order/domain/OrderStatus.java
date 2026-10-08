package com.example.delivery.order.domain;

/**
 * 주문 상태. 허용되는 전이는 Order의 상태 변경 메서드가 정한다 (01 5장).
 */
public enum OrderStatus {
    ORDERED,
    PAID,
    ACCEPTED,
    COMPLETED,
    CANCELED
}
