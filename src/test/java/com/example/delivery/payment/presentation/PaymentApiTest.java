package com.example.delivery.payment.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.support.ApiTestSupport;
import com.example.delivery.user.domain.UserRole;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PaymentApiTest extends ApiTestSupport {

    private String owner1;
    private String cust1;
    private String cust2;
    private long orderId;

    @BeforeEach
    void setUp() throws Exception {
        owner1 = signupAndLogin("owner1", UserRole.OWNER);
        cust1 = signupAndLogin("cust1", UserRole.CUSTOMER);
        cust2 = signupAndLogin("cust2", UserRole.CUSTOMER);
        long kimbapId = createMenuId(owner1, "김밥", 3500);
        orderId = createOrderId(cust1, kimbapId, 2);
    }

    @Nested
    @DisplayName("결제 POST /api/orders/{orderId}/payments")
    class Pay {

        @Test
        @DisplayName("본인 주문을 카드로 결제하면 201과 결제 금액 7,000원, 결제완료 주문 상태를 응답한다 (시나리오 #24)")
        void success() throws Exception {
            // when
            ResultActions result = pay(cust1, orderId);

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").isNumber())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.amount").value(7000))
                    .andExpect(jsonPath("$.method").value("CARD"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.orderStatus").value("PAID"))
                    .andExpect(jsonPath("$.createdAt").isString());
        }

        @Test
        @DisplayName("결제하면 결제 기록 1건이 주문 총액과 함께 문자열 상태로 저장된다 (발제 5-1 ⑥)")
        void savedRecord() throws Exception {
            // when
            pay(cust1, orderId);

            // then
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT amount, method, status FROM payments WHERE order_id = ?", orderId);
            assertThat(rows).containsExactly(Map.of("amount", 7000L, "method", "CARD", "status", "COMPLETED"));
        }

        @Test
        @DisplayName("요청에 금액을 넣어도 무시하고 주문 총액으로 결제한다")
        void ignoresRequestedAmount() throws Exception {
            // when
            ResultActions result = payWith(cust1, orderId, """
                    {"method": "CARD", "amount": 1}
                    """);

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(7000));
        }

        @Test
        @DisplayName("다른 손님의 주문을 결제하면 403 ORDER_ACCESS_DENIED (시나리오 #23)")
        void otherCustomer() throws Exception {
            // when
            ResultActions result = pay(cust2, orderId);

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("사장님이 결제하면 403")
        void owner() throws Exception {
            // when
            ResultActions result = pay(owner1, orderId);

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("토큰 없이 결제하면 403 (C-04 전)")
        void noToken() throws Exception {
            // when
            ResultActions result = mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"method": "CARD"}
                            """));

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("카드가 아닌 결제 수단이면 400")
        void notCard() throws Exception {
            // when
            ResultActions result = payWith(cust1, orderId, """
                    {"method": "CASH"}
                    """);

            // then
            expectInvalidField(result, "method");
        }

        @Test
        @DisplayName("결제 수단이 없으면 400")
        void methodMissing() throws Exception {
            // when
            ResultActions result = payWith(cust1, orderId, "{}");

            // then
            expectInvalidField(result, "method");
        }

        @Test
        @DisplayName("없는 주문을 결제하면 404 ORDER_NOT_FOUND")
        void notFound() throws Exception {
            // when
            ResultActions result = pay(cust1, 9999L);

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }

        @Test
        @DisplayName("같은 주문을 다시 결제하면 409이고 결제 기록은 1건 그대로다 (시나리오 #25)")
        void payAgain() throws Exception {
            // given
            pay(cust1, orderId);

            // when
            ResultActions result = pay(cust1, orderId);

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
            assertThat(countPayments()).isEqualTo(1);
        }

        @Test
        @DisplayName("취소된 주문을 결제하면 409 (시나리오 #33)")
        void canceledOrder() throws Exception {
            // given
            mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId)
                    .header(HttpHeaders.AUTHORIZATION, cust1));

            // when
            ResultActions result = pay(cust1, orderId);

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }

        @Test
        @DisplayName("같은 주문에 결제 요청 두 개가 동시에 오면 하나만 성공하고 결제 기록은 1건이다")
        void concurrentPayments() throws Exception {
            // given
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Callable<Integer> payTask = () -> {
                start.await();
                return pay(cust1, orderId).andReturn().getResponse().getStatus();
            };

            // when
            Future<Integer> first = executor.submit(payTask);
            Future<Integer> second = executor.submit(payTask);
            start.countDown();
            List<Integer> statuses = List.of(first.get(), second.get());
            executor.shutdown();

            // then
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            assertThat(countPayments()).isEqualTo(1);
            mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, cust1))
                    .andExpect(jsonPath("$[0].status").value("PAID"));
        }
    }

    @Nested
    @DisplayName("결제 이후 주문 상태")
    class AfterPayment {

        @Test
        @DisplayName("결제완료된 주문을 취소하면 409 (시나리오 #26)")
        void cancelPaidOrder() throws Exception {
            // given
            pay(cust1, orderId);

            // when
            ResultActions result = mockMvc.perform(patch("/api/orders/{orderId}/cancel", orderId)
                    .header(HttpHeaders.AUTHORIZATION, cust1));

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }
    }

    private ResultActions payWith(String token, long orderId, String body) throws Exception {
        return mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Integer countPayments() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
    }
}
