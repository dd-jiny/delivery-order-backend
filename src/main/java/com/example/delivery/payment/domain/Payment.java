package com.example.delivery.payment.domain;

import com.example.delivery.global.domain.BaseEntity;
import com.example.delivery.order.domain.Order;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private Payment(Order order, PaymentMethod method) {
        this.order = order;
        this.amount = order.getTotalPrice();
        this.method = method;
        this.status = PaymentStatus.COMPLETED;
    }

    /**
     * 완료된 결제 기록을 만든다. 금액은 인자로 받지 않고 주문 총액에서 가져와 요청 금액이 끼어들 수 없다.
     * 주문 상태 확인(order.pay())은 PaymentService가 먼저 한다 (04 D-10).
     */
    public static Payment complete(Order order, PaymentMethod method) {
        return new Payment(order, method);
    }
}
