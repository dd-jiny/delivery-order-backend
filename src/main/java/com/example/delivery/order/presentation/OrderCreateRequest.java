package com.example.delivery.order.presentation;

import com.example.delivery.order.application.dto.OrderCreateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 주문 생성 요청. 금액과 주문자는 받지 않는다 — 총액은 서버가 메뉴 가격으로 계산하고, 주문자는 토큰에서 꺼낸다.
 */
public record OrderCreateRequest(
        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Integer quantity,

        @NotBlank(message = "배송 주소는 필수입니다.")
        @Size(max = 255, message = "배송 주소는 255자 이하여야 합니다.")
        String deliveryAddress
) {

    public OrderCreateCommand toCommand() {
        return new OrderCreateCommand(menuId, quantity, deliveryAddress);
    }
}
