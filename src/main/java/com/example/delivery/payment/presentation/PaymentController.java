package com.example.delivery.payment.presentation;

import com.example.delivery.global.infrastructure.security.AuthUser;
import com.example.delivery.payment.application.PaymentFacade;
import com.example.delivery.payment.application.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할 검사(CUSTOMER)는 SecurityConfig의 URL 규칙이, "본인 주문만" 검사는 OrderService가 한다.
 */
@RestController
@RequestMapping("/api/orders/{orderId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request
    ) {
        return paymentFacade.pay(authUser.userId(), orderId, request.toCommand());
    }
}
