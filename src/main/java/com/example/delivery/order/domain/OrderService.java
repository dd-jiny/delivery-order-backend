package com.example.delivery.order.domain;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 주문 도메인 서비스. 조회·존재 확인(404)·소유 확인(403)·저장처럼 Repository가 필요한 주문 단위 작업을 맡고,
 * 주문·결제 Facade가 재사용한다. 상태 전이(409)와 총액 계산은 Order 엔티티에 있다. 트랜잭션은 Facade가 연다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    /** 품절 확인(409)은 Order.create()가 한다. 삭제된 메뉴(404)는 Facade가 MenuService로 먼저 거른다. */
    public Order create(User customer, Menu menu, int quantity, String deliveryAddress) {
        return orderRepository.save(Order.create(customer, menu, quantity, deliveryAddress));
    }

    /** 손님은 자신이 한 주문, 사장님은 자신의 메뉴에 들어온 주문을 본다. */
    public List<Order> getOrders(Long userId, UserRole role) {
        return switch (role) {
            case CUSTOMER -> orderRepository.findAllByCustomerId(userId);
            case OWNER -> orderRepository.findAllByMenuOwnerId(userId);
        };
    }

    /** 존재 확인(404)을 먼저 하고 주문자 확인(403)을 한다 (01 D-01). 결제에서도 재사용한다. */
    public Order getCustomerOrder(Long customerId, Long orderId) {
        Order order = getOrder(orderId);
        if (!order.isOrderedBy(customerId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    /** 존재 확인(404)을 먼저 하고 메뉴 주인 확인(403)을 한다 (01 D-01). */
    public Order getOwnerOrder(Long ownerId, Long orderId) {
        Order order = getOrder(orderId);
        if (!order.isMenuOwnedBy(ownerId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    public Order cancel(Long customerId, Long orderId) {
        Order order = getCustomerOrder(customerId, orderId);
        order.cancel();
        return order;
    }

    public Order accept(Long ownerId, Long orderId) {
        Order order = getOwnerOrder(ownerId, orderId);
        order.accept();
        return order;
    }

    public Order complete(Long ownerId, Long orderId) {
        Order order = getOwnerOrder(ownerId, orderId);
        order.complete();
        return order;
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
