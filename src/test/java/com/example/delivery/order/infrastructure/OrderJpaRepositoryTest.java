package com.example.delivery.order.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuFixture;
import com.example.delivery.menu.infrastructure.MenuJpaRepository;
import com.example.delivery.order.domain.Order;
import com.example.delivery.order.domain.OrderFixture;
import com.example.delivery.support.RepositoryTestSupport;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.domain.UserRole;
import com.example.delivery.user.infrastructure.UserJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderJpaRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private MenuJpaRepository menuRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner1;
    private User owner2;
    private User cust1;
    private User cust2;
    private Menu kimbap;
    private Menu ramen;

    @BeforeEach
    void setUp() {
        owner1 = userRepository.save(UserFixture.owner());
        owner2 = userRepository.save(User.create("owner2", UserFixture.ENCODED_PASSWORD, UserRole.OWNER));
        cust1 = userRepository.save(UserFixture.customer());
        cust2 = userRepository.save(User.create("cust2", UserFixture.ENCODED_PASSWORD, UserRole.CUSTOMER));
        kimbap = menuRepository.save(MenuFixture.menu(owner1, "김밥"));
        ramen = menuRepository.save(MenuFixture.menu(owner2, "라면"));
    }

    @Test
    @DisplayName("손님의 주문 목록은 그 손님이 한 주문만 담는다")
    void findAllByCustomerId_onlyOwnOrders() {
        // given
        orderRepository.save(OrderFixture.order(cust1, kimbap));
        orderRepository.save(OrderFixture.order(cust1, ramen));
        orderRepository.save(OrderFixture.order(cust2, kimbap));

        // when
        List<Order> orders = orderRepository.findAllByCustomerIdOrderByCreatedAtDescIdDesc(cust1.getId());

        // then
        assertThat(orders).extracting(Order::getMenuName).containsExactlyInAnyOrder("김밥", "라면");
    }

    @Test
    @DisplayName("손님의 주문 목록은 주문 시각이 늦은 순서다")
    void findAllByCustomerId_latestFirst() {
        // given
        Order first = orderRepository.saveAndFlush(OrderFixture.order(cust1, kimbap));
        orderRepository.saveAndFlush(OrderFixture.order(cust1, ramen));
        makeLatest(first);

        // when
        List<Order> orders = orderRepository.findAllByCustomerIdOrderByCreatedAtDescIdDesc(cust1.getId());

        // then
        assertThat(orders).extracting(Order::getMenuName).containsExactly("김밥", "라면");
    }

    @Test
    @DisplayName("사장님의 주문 목록은 그 사장님 메뉴에 들어온 주문만 담는다")
    void findAllByMenuOwnerId_onlyOwnMenus() {
        // given
        orderRepository.save(OrderFixture.order(cust1, kimbap));
        orderRepository.save(OrderFixture.order(cust2, kimbap));
        orderRepository.save(OrderFixture.order(cust1, ramen));

        // when
        List<Order> orders = orderRepository.findAllByMenuOwnerIdOrderByCreatedAtDescIdDesc(owner1.getId());

        // then
        assertThat(orders).extracting(order -> order.getCustomer().getUsername())
                .containsExactlyInAnyOrder("cust1", "cust2");
        assertThat(orders).extracting(Order::getMenuName).containsOnly("김밥");
    }

    @Test
    @DisplayName("사장님의 주문 목록은 삭제된 메뉴의 주문도 담는다")
    void findAllByMenuOwnerId_includesDeletedMenu() {
        // given
        orderRepository.save(OrderFixture.order(cust1, kimbap));
        kimbap.delete(LocalDateTime.of(2026, 10, 7, 14, 30));
        entityManager.flush();

        // when
        List<Order> orders = orderRepository.findAllByMenuOwnerIdOrderByCreatedAtDescIdDesc(owner1.getId());

        // then
        assertThat(orders).extracting(Order::getMenuName).containsExactly("김밥");
    }

    @Test
    @DisplayName("사장님의 주문 목록은 주문 시각이 늦은 순서다")
    void findAllByMenuOwnerId_latestFirst() {
        // given
        Order first = orderRepository.saveAndFlush(OrderFixture.order(cust1, kimbap));
        orderRepository.saveAndFlush(OrderFixture.order(cust2, kimbap));
        makeLatest(first);

        // when
        List<Order> orders = orderRepository.findAllByMenuOwnerIdOrderByCreatedAtDescIdDesc(owner1.getId());

        // then
        assertThat(orders).extracting(order -> order.getCustomer().getUsername()).containsExactly("cust1", "cust2");
    }

    @Test
    @DisplayName("손님의 주문 목록은 주문자를 함께 조회해 주문마다 회원 조회가 추가로 나가지 않는다")
    void findAllByCustomerId_fetchesCustomer() {
        // given
        orderRepository.save(OrderFixture.order(cust1, kimbap));
        entityManager.flush();
        entityManager.clear();

        // when
        List<Order> orders = orderRepository.findAllByCustomerIdOrderByCreatedAtDescIdDesc(cust1.getId());

        // then
        assertThat(Hibernate.isInitialized(orders.getFirst().getCustomer())).isTrue();
    }

    @Test
    @DisplayName("사장님의 주문 목록은 주문자를 함께 조회해 주문마다 회원 조회가 추가로 나가지 않는다")
    void findAllByMenuOwnerId_fetchesCustomer() {
        // given
        orderRepository.save(OrderFixture.order(cust1, kimbap));
        entityManager.flush();
        entityManager.clear();

        // when
        List<Order> orders = orderRepository.findAllByMenuOwnerIdOrderByCreatedAtDescIdDesc(owner1.getId());

        // then
        assertThat(Hibernate.isInitialized(orders.getFirst().getCustomer())).isTrue();
    }

    @Test
    @DisplayName("주문 상태는 DB에 문자열로 저장되고 버전은 0에서 시작한다")
    void statusStoredAsString() {
        // given
        Order order = orderRepository.saveAndFlush(OrderFixture.order(cust1, kimbap));

        // when
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, version FROM orders WHERE id = ?", order.getId());

        // then
        assertThat(row).containsEntry("status", "ORDERED").containsEntry("version", 0L);
    }

    /** 먼저 저장한 주문의 주문 시각을 가장 늦게 바꿔, 정렬이 id가 아니라 주문 시각을 따르는지 확인한다. */
    private void makeLatest(Order order) {
        jdbcTemplate.update("UPDATE orders SET created_at = '2099-01-01 00:00:00' WHERE id = ?", order.getId());
        entityManager.clear();
    }
}
