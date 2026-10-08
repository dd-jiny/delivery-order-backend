package com.example.delivery.order.infrastructure;

import com.example.delivery.order.domain.Order;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 목록 응답에 주문자 아이디(customerUsername)가 들어가므로, 주문마다 회원 조회가 나가지 않게 customer를 함께 조회한다.
 * 같은 시각이면 나중에 저장된(id가 큰) 주문이 먼저 온다.
 */
public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "customer")
    List<Order> findAllByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    /** orders.menu.owner.id를 따라가므로 삭제된 메뉴의 주문도 조회된다. */
    @EntityGraph(attributePaths = "customer")
    List<Order> findAllByMenuOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);
}
