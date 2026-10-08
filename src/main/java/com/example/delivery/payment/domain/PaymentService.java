package com.example.delivery.payment.domain;

import com.example.delivery.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 결제 도메인 서비스. 주문은 직접 조회하지 않고 Facade가 OrderService로 받아 넘긴다(본인 주문 404 → 403).
 * 트랜잭션은 Facade가 열어 주문 상태 변경과 결제 기록 저장이 함께 커밋·롤백된다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 상태 판단은 Order, 결제 기록은 Payment가 맡고 이 서비스는 순서만 지시한다 (04 D-10).
     * order.pay()가 409로 거절하면 결제 기록을 만들지 않는다.
     * 동시 결제는 둘 다 pay()를 통과할 수 있는데, 커밋 시 Order의 @Version이 나중 요청을 막는다 (02 D-04).
     */
    public Payment pay(Order order, PaymentMethod method) {
        order.pay();
        return paymentRepository.save(Payment.complete(order, method));
    }
}
