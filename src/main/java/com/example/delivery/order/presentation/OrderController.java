package com.example.delivery.order.presentation;

import com.example.delivery.global.infrastructure.security.AuthUser;
import com.example.delivery.order.application.OrderFacade;
import com.example.delivery.order.application.dto.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할 검사(CUSTOMER·OWNER)는 SecurityConfig의 URL 규칙이, "본인 주문만" 검사는 OrderService가 한다.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody OrderCreateRequest request) {
        return orderFacade.createOrder(authUser.userId(), request.toCommand());
    }

    /** 손님은 자신이 한 주문, 사장님은 자신의 메뉴에 들어온 주문을 최신순으로 받는다. */
    @GetMapping
    public List<OrderResponse> getOrders(@AuthenticationPrincipal AuthUser authUser) {
        return orderFacade.getOrders(authUser.userId(), authUser.role());
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long orderId) {
        return orderFacade.cancel(authUser.userId(), orderId);
    }

    @PatchMapping("/{orderId}/accept")
    public OrderResponse accept(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long orderId) {
        return orderFacade.accept(authUser.userId(), orderId);
    }

    @PatchMapping("/{orderId}/complete")
    public OrderResponse complete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long orderId) {
        return orderFacade.complete(authUser.userId(), orderId);
    }
}
