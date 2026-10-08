package com.example.delivery.payment.presentation;

import com.example.delivery.payment.application.dto.PaymentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 결제 요청. 금액은 받지 않는다 — 결제 금액은 서버가 주문 총액으로 정한다.
 * 허용값은 PaymentMethod enum과 같아야 한다. enum 값을 바꾸면 @Pattern도 함께 고친다 (04 D-32).
 */
public record PaymentRequest(
        @NotBlank(message = "결제 수단은 필수입니다.")
        @Pattern(regexp = "CARD", message = "결제 수단은 CARD만 가능합니다.")
        String method
) {

    public PaymentCommand toCommand() {
        return new PaymentCommand(method);
    }
}
