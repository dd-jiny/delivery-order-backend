package com.example.delivery.order.domain;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.user.domain.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트용 주문 생성. 테스트에 중요한 값만 인자로 받는다.
 */
public class OrderFixture {

    public static final String ADDRESS = "서울시 강남구 테헤란로 1";

    public static Order order(User customer, Menu menu) {
        return Order.create(customer, menu, 2, ADDRESS);
    }

    public static Order withId(Order order, Long id) {
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    /** 결제·수락 API를 거치지 않고 원하는 상태의 주문을 만든다. */
    public static Order withStatus(Order order, OrderStatus status) {
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }
}
