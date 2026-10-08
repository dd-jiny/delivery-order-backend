package com.example.delivery.order.domain;

import com.example.delivery.global.domain.BaseEntity;
import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false, length = 100)
    private String menuName;

    @Column(nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Long totalPrice;

    @Column(nullable = false)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    private Order(User customer, Menu menu, int quantity, String deliveryAddress) {
        this.customer = customer;
        this.menu = menu;
        this.menuName = menu.getName();
        this.unitPrice = menu.getPrice();
        this.quantity = quantity;
        this.totalPrice = unitPrice * quantity;
        this.deliveryAddress = deliveryAddress;
        this.status = OrderStatus.ORDERED;
    }

    /**
     * 메뉴 이름·가격은 주문 시점 값으로 복사(스냅샷)해 이후 메뉴가 수정·삭제되어도 주문 기록이 바뀌지 않는다.
     * 총액은 요청이 아니라 메뉴 가격으로 계산한다. 삭제된 메뉴(404)는 호출하는 쪽이 먼저 거른다.
     */
    public static Order create(User customer, Menu menu, int quantity, String deliveryAddress) {
        if (!menu.isOnSale()) {
            throw new BusinessException(ErrorCode.MENU_SOLD_OUT);
        }
        return new Order(customer, menu, quantity, deliveryAddress);
    }

    /** ORDERED → PAID. 결제 API(PaymentService)를 통해서만 호출한다. */
    public void pay() {
        changeStatus(OrderStatus.ORDERED, OrderStatus.PAID);
    }

    /** ORDERED → CANCELED. 결제 후에는 취소할 수 없다. */
    public void cancel() {
        changeStatus(OrderStatus.ORDERED, OrderStatus.CANCELED);
    }

    /** PAID → ACCEPTED */
    public void accept() {
        changeStatus(OrderStatus.PAID, OrderStatus.ACCEPTED);
    }

    /** ACCEPTED → COMPLETED */
    public void complete() {
        changeStatus(OrderStatus.ACCEPTED, OrderStatus.COMPLETED);
    }

    /** customer는 지연 로딩 프록시여도 getId()는 조회 없이 읽힌다. */
    public boolean isOrderedBy(Long userId) {
        return customer.getId().equals(userId);
    }

    public boolean isMenuOwnedBy(Long userId) {
        return menu.isOwnedBy(userId);
    }

    /** 허용된 전이는 현재 상태 하나에서 다음 상태 하나로만 간다. 그 외는 모두 409로 거절한다. */
    private void changeStatus(OrderStatus expected, OrderStatus next) {
        if (status != expected) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = next;
    }
}
