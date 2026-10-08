package com.example.delivery.order.domain;

import java.util.List;
import java.util.Optional;

/**
 * 주문 저장소. domain은 "무엇이 필요한지"만 정하고, DB 접근 구현은 infrastructure(OrderRepositoryImpl)가 맡는다.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    /** 손님이 한 주문을 최신순으로 조회한다. */
    List<Order> findAllByCustomerId(Long customerId);

    /** 사장님 메뉴에 들어온 주문을 최신순으로 조회한다. 삭제된 메뉴의 주문도 포함한다. */
    List<Order> findAllByMenuOwnerId(Long ownerId);
}
