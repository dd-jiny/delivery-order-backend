package com.example.delivery.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.user.domain.UserRole;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * E2E 테스트 공통 부모. Security 필터를 포함한 전체 앱을 띄우고 MockMvc로 요청한다.
 * 롤백(@Transactional) 대신 각 테스트 전에 모든 테이블을 비워 테스트끼리 독립시킨다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.truncateAllTables(jdbcTemplate);
    }

    /** 회원가입 후 로그인해 Authorization 헤더 값("Bearer ...")을 돌려준다. */
    protected String signupAndLogin(String username, UserRole role) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "password": "%s", "role": "%s"}
                        """.formatted(username, PASSWORD, role)));
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    /** 메뉴를 등록하고 메뉴 ID를 돌려준다. */
    protected long createMenuId(String ownerToken, String name, long price) throws Exception {
        String body = mockMvc.perform(post("/api/menus")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "price": %d}
                                """.formatted(name, price)))
                .andReturn().getResponse().getContentAsString();
        return readId(body, "$.menuId");
    }

    /** 주문을 생성하고 주문 ID를 돌려준다. */
    protected long createOrderId(String customerToken, long menuId, int quantity) throws Exception {
        String body = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"menuId": %d, "quantity": %d, "deliveryAddress": "서울시 강남구 테헤란로 1"}
                                """.formatted(menuId, quantity)))
                .andReturn().getResponse().getContentAsString();
        return readId(body, "$.orderId");
    }

    /** 카드로 결제한다. */
    protected ResultActions pay(String customerToken, long orderId) throws Exception {
        return mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                .header(HttpHeaders.AUTHORIZATION, customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"method": "CARD"}
                        """));
    }

    protected static long readId(String body, String path) {
        return ((Number) JsonPath.read(body, path)).longValue();
    }

    /** 입력 검증 실패(400)이고 첫 번째 필드 오류가 field인지 확인한다. */
    protected static void expectInvalidField(ResultActions result, String field) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field));
    }
}
