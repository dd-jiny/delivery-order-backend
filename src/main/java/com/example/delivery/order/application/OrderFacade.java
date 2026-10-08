package com.example.delivery.order.application;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuService;
import com.example.delivery.order.application.dto.OrderCreateCommand;
import com.example.delivery.order.application.dto.OrderResponse;
import com.example.delivery.order.domain.Order;
import com.example.delivery.order.domain.OrderService;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserRole;
import com.example.delivery.user.domain.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 유스케이스 흐름을 조율한다. 트랜잭션을 열고, 도메인 서비스를 순서대로 호출하고, 엔티티를 응답 DTO로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final MenuService menuService;
    private final UserService userService;
    private final OrderService orderService;

    /** 메뉴 404(삭제 포함)를 먼저 확인하고, 품절 409는 주문을 만들 때 Order가 확인한다. */
    @Transactional
    public OrderResponse createOrder(Long customerId, OrderCreateCommand command) {
        Menu menu = menuService.getMenu(command.menuId());
        User customer = userService.getReference(customerId);
        Order order = orderService.create(customer, menu, command.quantity(), command.deliveryAddress());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(Long userId, UserRole role) {
        return orderService.getOrders(userId, role).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse cancel(Long customerId, Long orderId) {
        return OrderResponse.from(orderService.cancel(customerId, orderId));
    }

    @Transactional
    public OrderResponse accept(Long ownerId, Long orderId) {
        return OrderResponse.from(orderService.accept(ownerId, orderId));
    }

    @Transactional
    public OrderResponse complete(Long ownerId, Long orderId) {
        return OrderResponse.from(orderService.complete(ownerId, orderId));
    }
}
