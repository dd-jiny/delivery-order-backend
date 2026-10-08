package com.example.delivery.order.application.dto;

import com.example.delivery.order.domain.Order;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 주문 응답. 메뉴 이름·단가는 주문 당시의 스냅샷이고, domain enum(OrderStatus)은 문자열로 바꾼다.
 */
public record OrderResponse(
        Long orderId,
        String customerUsername,
        Long menuId,
        String menuName,
        Long unitPrice,
        int quantity,
        Long totalPrice,
        String deliveryAddress,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    /**
     * 트랜잭션 안에서 호출한다. customer는 지연 로딩이라 username을 읽을 때 조회된다(목록은 @EntityGraph로 함께 조회).
     * menu는 getId()만 읽으므로 조회되지 않는다.
     */
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getUsername(),
                order.getMenu().getId(),
                order.getMenuName(),
                order.getUnitPrice(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getDeliveryAddress(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
