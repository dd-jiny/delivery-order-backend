package com.example.delivery.order.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.support.ApiTestSupport;
import com.example.delivery.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 수락·배달완료의 성공 흐름은 결제 API가 필요해 결제 E2E(4단계)에서 검증한다.
 */
class OrderApiTest extends ApiTestSupport {

    private static final String ADDRESS = "서울시 강남구 테헤란로 1";

    private String owner1;
    private String owner2;
    private String cust1;
    private String cust2;
    private long kimbapId;

    @BeforeEach
    void setUp() throws Exception {
        owner1 = signupAndLogin("owner1", UserRole.OWNER);
        owner2 = signupAndLogin("owner2", UserRole.OWNER);
        cust1 = signupAndLogin("cust1", UserRole.CUSTOMER);
        cust2 = signupAndLogin("cust2", UserRole.CUSTOMER);
        kimbapId = createMenuId(owner1, "김밥", 3500);
    }

    @Nested
    @DisplayName("주문 생성 POST /api/orders")
    class Create {

        @Test
        @DisplayName("손님이 주문하면 201과 서버가 계산한 총액, 주문요청 상태를 응답한다 (시나리오 #18)")
        void success() throws Exception {
            // when
            ResultActions result = createOrder(cust1, kimbapId, 2);

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").isNumber())
                    .andExpect(jsonPath("$.customerUsername").value("cust1"))
                    .andExpect(jsonPath("$.menuId").value(kimbapId))
                    .andExpect(jsonPath("$.menuName").value("김밥"))
                    .andExpect(jsonPath("$.unitPrice").value(3500))
                    .andExpect(jsonPath("$.quantity").value(2))
                    .andExpect(jsonPath("$.totalPrice").value(7000))
                    .andExpect(jsonPath("$.deliveryAddress").value(ADDRESS))
                    .andExpect(jsonPath("$.status").value("ORDERED"))
                    .andExpect(jsonPath("$.createdAt").isString())
                    .andExpect(jsonPath("$.updatedAt").isString());
        }

        @Test
        @DisplayName("요청에 금액을 넣어도 무시하고 메뉴 가격으로 총액을 계산한다")
        void ignoresRequestedPrice() throws Exception {
            // when
            ResultActions result = mockMvc.perform(post("/api/orders")
                    .header(HttpHeaders.AUTHORIZATION, cust1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"menuId": %d, "quantity": 2, "deliveryAddress": "%s", "totalPrice": 1}
                            """.formatted(kimbapId, ADDRESS)));

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalPrice").value(7000));
        }

        @Test
        @DisplayName("토큰 없이 주문하면 403 (C-04 전)")
        void noToken() throws Exception {
            // when
            ResultActions result = mockMvc.perform(post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(orderBody(kimbapId, 2, ADDRESS)));

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("사장님이 주문하면 403 (시나리오 #17)")
        void owner() throws Exception {
            // when
            ResultActions result = createOrder(owner1, kimbapId, 2);

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("수량이 0이면 400 (시나리오 #19)")
        void quantityZero() throws Exception {
            // when
            ResultActions result = createOrder(cust1, kimbapId, 0);

            // then
            expectInvalidField(result, "quantity");
        }

        @Test
        @DisplayName("메뉴 ID가 없으면 400")
        void menuIdMissing() throws Exception {
            // when
            ResultActions result = createOrder(cust1, """
                    {"quantity": 2, "deliveryAddress": "%s"}
                    """.formatted(ADDRESS));

            // then
            expectInvalidField(result, "menuId");
        }

        @Test
        @DisplayName("배송 주소가 비어 있으면 400")
        void addressBlank() throws Exception {
            // when
            ResultActions result = createOrder(cust1, orderBody(kimbapId, 2, " "));

            // then
            expectInvalidField(result, "deliveryAddress");
        }

        @Test
        @DisplayName("배송 주소가 255자를 넘으면 400")
        void addressTooLong() throws Exception {
            // when
            ResultActions result = createOrder(cust1, orderBody(kimbapId, 2, "가".repeat(256)));

            // then
            expectInvalidField(result, "deliveryAddress");
        }

        @Test
        @DisplayName("없는 메뉴를 주문하면 404 MENU_NOT_FOUND")
        void menuNotFound() throws Exception {
            // when
            ResultActions result = createOrder(cust1, 9999L, 2);

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        }

        @Test
        @DisplayName("삭제된 메뉴를 주문하면 404 MENU_NOT_FOUND (시나리오 #38)")
        void deletedMenu() throws Exception {
            // given
            deleteMenu(owner1, kimbapId);

            // when
            ResultActions result = createOrder(cust1, kimbapId, 1);

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        }

        @Test
        @DisplayName("품절 메뉴를 주문하면 409 MENU_SOLD_OUT")
        void soldOut() throws Exception {
            // given (판매 상태 변경 API가 없어 DB에서 직접 바꾼다)
            jdbcTemplate.update("UPDATE menus SET status = 'SOLD_OUT' WHERE id = ?", kimbapId);

            // when
            ResultActions result = createOrder(cust1, kimbapId, 1);

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("MENU_SOLD_OUT"));
        }
    }

    @Nested
    @DisplayName("주문 목록 GET /api/orders")
    class GetOrders {

        @Test
        @DisplayName("손님은 자신이 한 주문만 보고, 주문이 없으면 빈 목록이다 (시나리오 #20)")
        void customer() throws Exception {
            // given
            createOrder(cust1, kimbapId, 2);

            // when & then
            getOrders(cust1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].customerUsername").value("cust1"))
                    .andExpect(jsonPath("$[0].totalPrice").value(7000));
            getOrders(cust2)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("사장님은 자신의 메뉴에 들어온 주문만 보고, 없으면 빈 목록이다 (시나리오 #21)")
        void owner() throws Exception {
            // given
            createOrder(cust1, kimbapId, 2);

            // when & then
            getOrders(owner1)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].customerUsername").value("cust1"))
                    .andExpect(jsonPath("$[0].menuName").value("김밥"));
            getOrders(owner2)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("최신 주문이 먼저 온다")
        void latestFirst() throws Exception {
            // given
            createOrder(cust1, kimbapId, 1);
            createOrder(cust1, kimbapId, 3);

            // when & then
            getOrders(cust1)
                    .andExpect(jsonPath("$[0].quantity").value(3))
                    .andExpect(jsonPath("$[1].quantity").value(1));
        }

        @Test
        @DisplayName("메뉴가 삭제되어도 손님과 사장님 모두 주문 기록을 그대로 본다 (시나리오 #39)")
        void deletedMenuOrderRemains() throws Exception {
            // given
            createOrder(cust1, kimbapId, 2);
            deleteMenu(owner1, kimbapId);

            // when & then
            getOrders(cust1)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].menuId").value(kimbapId))
                    .andExpect(jsonPath("$[0].menuName").value("김밥"));
            getOrders(owner1)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].menuName").value("김밥"));
        }

        @Test
        @DisplayName("토큰 없이 조회하면 403 (C-04 전)")
        void noToken() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/orders"));

            // then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("주문 취소 PATCH /api/orders/{orderId}/cancel")
    class Cancel {

        @Test
        @DisplayName("본인 주문을 취소하면 200과 주문취소 상태를 응답하고, 목록에서도 취소 상태로 보인다 (시나리오 #30, #32)")
        void success() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 1);

            // when
            ResultActions result = changeStatus(cust1, orderId, "cancel");

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.status").value("CANCELED"));
            getOrders(cust1)
                    .andExpect(jsonPath("$[0].status").value("CANCELED"));
        }

        @Test
        @DisplayName("다른 손님의 주문을 취소하면 403 ORDER_ACCESS_DENIED (시나리오 #31)")
        void otherCustomer() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 1);

            // when
            ResultActions result = changeStatus(cust2, orderId, "cancel");

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("사장님이 취소하면 403")
        void owner() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 1);

            // when
            ResultActions result = changeStatus(owner1, orderId, "cancel");

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("없는 주문을 취소하면 404 ORDER_NOT_FOUND")
        void notFound() throws Exception {
            // when
            ResultActions result = changeStatus(cust1, 9999L, "cancel");

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }

        @Test
        @DisplayName("이미 취소한 주문을 다시 취소하면 409 INVALID_ORDER_STATUS")
        void alreadyCanceled() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 1);
            changeStatus(cust1, orderId, "cancel");

            // when
            ResultActions result = changeStatus(cust1, orderId, "cancel");

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }

        @Test
        @DisplayName("주문 ID가 숫자가 아니면 400")
        void invalidId() throws Exception {
            // when
            ResultActions result = mockMvc.perform(patch("/api/orders/abc/cancel")
                    .header(HttpHeaders.AUTHORIZATION, cust1));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    @Nested
    @DisplayName("주문 수락 PATCH /api/orders/{orderId}/accept")
    class Accept {

        @Test
        @DisplayName("결제 전 주문을 수락하면 409 INVALID_ORDER_STATUS (시나리오 #22)")
        void notPaid() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 2);

            // when
            ResultActions result = changeStatus(owner1, orderId, "accept");

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }

        @Test
        @DisplayName("다른 사장님 메뉴의 주문을 수락하면 403 ORDER_ACCESS_DENIED")
        void otherOwner() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 2);

            // when
            ResultActions result = changeStatus(owner2, orderId, "accept");

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("손님이 수락하면 403")
        void customer() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 2);

            // when
            ResultActions result = changeStatus(cust1, orderId, "accept");

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("없는 주문을 수락하면 404 ORDER_NOT_FOUND")
        void notFound() throws Exception {
            // when
            ResultActions result = changeStatus(owner1, 9999L, "accept");

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("주문 배달완료 PATCH /api/orders/{orderId}/complete")
    class Complete {

        @Test
        @DisplayName("수락되지 않은 주문을 배달완료하면 409 INVALID_ORDER_STATUS")
        void notAccepted() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 2);

            // when
            ResultActions result = changeStatus(owner1, orderId, "complete");

            // then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS"));
        }

        @Test
        @DisplayName("다른 사장님 메뉴의 주문을 배달완료하면 403 ORDER_ACCESS_DENIED")
        void otherOwner() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 2);

            // when
            ResultActions result = changeStatus(owner2, orderId, "complete");

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("손님이 배달완료하면 403")
        void customer() throws Exception {
            // given
            long orderId = createOrderId(cust1, kimbapId, 2);

            // when
            ResultActions result = changeStatus(cust1, orderId, "complete");

            // then
            result.andExpect(status().isForbidden());
        }
    }

    private long createMenuId(String token, String name, long price) throws Exception {
        String body = mockMvc.perform(post("/api/menus")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "price": %d}
                                """.formatted(name, price)))
                .andReturn().getResponse().getContentAsString();
        return readId(body, "$.menuId");
    }

    private void deleteMenu(String token, long menuId) throws Exception {
        mockMvc.perform(delete("/api/menus/{menuId}", menuId)
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private static String orderBody(long menuId, int quantity, String address) {
        return """
                {"menuId": %d, "quantity": %d, "deliveryAddress": "%s"}
                """.formatted(menuId, quantity, address);
    }

    private ResultActions createOrder(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions createOrder(String token, long menuId, int quantity) throws Exception {
        return createOrder(token, orderBody(menuId, quantity, ADDRESS));
    }

    private long createOrderId(String token, long menuId, int quantity) throws Exception {
        return readId(createOrder(token, menuId, quantity).andReturn().getResponse().getContentAsString(), "$.orderId");
    }

    private ResultActions getOrders(String token) throws Exception {
        return mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private ResultActions changeStatus(String token, long orderId, String action) throws Exception {
        return mockMvc.perform(patch("/api/orders/{orderId}/{action}", orderId, action)
                .header(HttpHeaders.AUTHORIZATION, token));
    }
}
