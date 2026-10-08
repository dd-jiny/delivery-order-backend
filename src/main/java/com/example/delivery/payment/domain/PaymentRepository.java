package com.example.delivery.payment.domain;

/**
 * 결제 저장소. domain은 "무엇이 필요한지"만 정하고, DB 접근 구현은 infrastructure(PaymentRepositoryImpl)가 맡는다.
 */
public interface PaymentRepository {

    Payment save(Payment payment);
}
