package com.example.delivery.order.infrastructure;

import com.example.delivery.order.domain.Order;
import com.example.delivery.order.domain.OrderRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public List<Order> findAllByCustomerId(Long customerId) {
        return orderJpaRepository.findAllByCustomerIdOrderByCreatedAtDescIdDesc(customerId);
    }

    @Override
    public List<Order> findAllByMenuOwnerId(Long ownerId) {
        return orderJpaRepository.findAllByMenuOwnerIdOrderByCreatedAtDescIdDesc(ownerId);
    }
}
