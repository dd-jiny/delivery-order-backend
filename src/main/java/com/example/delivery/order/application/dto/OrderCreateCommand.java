package com.example.delivery.order.application.dto;

/**
 * 주문 생성 입력. 형식 검증은 presentation(OrderCreateRequest)에서 끝난 값이다. 금액은 받지 않는다.
 */
public record OrderCreateCommand(Long menuId, int quantity, String deliveryAddress) {
}
