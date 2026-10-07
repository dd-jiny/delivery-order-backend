package com.example.delivery.menu.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

class MenuApiTest extends ApiTestSupport {

    private static final String KIMBAP = """
            {"name": "김밥", "price": 3000, "description": "참기름 향 가득한 기본 김밥"}
            """;

    private String owner1;
    private String owner2;
    private String cust1;

    @BeforeEach
    void setUp() throws Exception {
        owner1 = signupAndLogin("owner1", UserRole.OWNER);
        owner2 = signupAndLogin("owner2", UserRole.OWNER);
        cust1 = signupAndLogin("cust1", UserRole.CUSTOMER);
    }

    @Nested
    @DisplayName("메뉴 등록 POST /api/menus")
    class Create {

        @Test
        @DisplayName("사장님이 등록하면 201과 판매 중인 메뉴를 응답한다 (시나리오 #11)")
        void success() throws Exception {
            // when
            ResultActions result = createMenu(owner1, KIMBAP);

            // then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.menuId").isNumber())
                    .andExpect(jsonPath("$.ownerId").value(1))
                    .andExpect(jsonPath("$.name").value("김밥"))
                    .andExpect(jsonPath("$.price").value(3000))
                    .andExpect(jsonPath("$.description").value("참기름 향 가득한 기본 김밥"))
                    .andExpect(jsonPath("$.status").value("ON_SALE"))
                    .andExpect(jsonPath("$.createdAt").isString())
                    .andExpect(jsonPath("$.updatedAt").isString());
        }

        @Test
        @DisplayName("토큰 없이 등록하면 403 (시나리오 #9, C-04 전)")
        void noToken() throws Exception {
            // when
            ResultActions result = mockMvc.perform(post("/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(KIMBAP));

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("손님이 등록하면 403 (시나리오 #10)")
        void customer() throws Exception {
            // when
            ResultActions result = createMenu(cust1, KIMBAP);

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("가격이 0원이면 400 (시나리오 #12)")
        void priceZero() throws Exception {
            // when
            ResultActions result = createMenu(owner1, """
                    {"name": "김밥", "price": 0}
                    """);

            // then
            expectInvalidField(result, "price");
        }

        @Test
        @DisplayName("이름이 비어 있으면 400")
        void nameBlank() throws Exception {
            // when
            ResultActions result = createMenu(owner1, """
                    {"name": " ", "price": 3000}
                    """);

            // then
            expectInvalidField(result, "name");
        }

        @Test
        @DisplayName("이름이 100자를 넘으면 400")
        void nameTooLong() throws Exception {
            // when
            ResultActions result = createMenu(owner1, """
                    {"name": "%s", "price": 3000}
                    """.formatted("가".repeat(101)));

            // then
            expectInvalidField(result, "name");
        }

        @Test
        @DisplayName("설명이 500자를 넘으면 400")
        void descriptionTooLong() throws Exception {
            // when
            ResultActions result = createMenu(owner1, """
                    {"name": "김밥", "price": 3000, "description": "%s"}
                    """.formatted("가".repeat(501)));

            // then
            expectInvalidField(result, "description");
        }
    }

    @Nested
    @DisplayName("메뉴 목록 조회 GET /api/menus")
    class GetMenus {

        @Test
        @DisplayName("토큰 없이 조회하면 200과 페이지 응답을 준다 (시나리오 #13)")
        void success() throws Exception {
            // given
            createMenu(owner1, KIMBAP);

            // when
            ResultActions result = mockMvc.perform(get("/api/menus"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("김밥"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("여러 사장님의 메뉴를 최신 등록순으로 보여준다")
        void latestFirst() throws Exception {
            // given
            createMenu(owner1, """
                    {"name": "김밥", "price": 3000}
                    """);
            createMenu(owner2, """
                    {"name": "라면", "price": 4000}
                    """);

            // when
            ResultActions result = mockMvc.perform(get("/api/menus"));

            // then
            result.andExpect(jsonPath("$.content[0].name").value("라면"))
                    .andExpect(jsonPath("$.content[1].name").value("김밥"));
        }

        @Test
        @DisplayName("페이지 크기는 최대 50으로 보정된다")
        void maxPageSize() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/menus").param("size", "100"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(50));
        }
    }

    @Nested
    @DisplayName("메뉴 단건 조회 GET /api/menus/{menuId}")
    class GetMenu {

        @Test
        @DisplayName("토큰 없이 조회하면 200 (시나리오 #14)")
        void success() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = mockMvc.perform(get("/api/menus/{menuId}", menuId));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.menuId").value(menuId))
                    .andExpect(jsonPath("$.name").value("김밥"))
                    .andExpect(jsonPath("$.status").value("ON_SALE"));
        }

        @Test
        @DisplayName("없는 메뉴면 404 MENU_NOT_FOUND (시나리오 #14)")
        void notFound() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/menus/{menuId}", 9999));

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        }

        @Test
        @DisplayName("메뉴 ID가 숫자가 아니면 400")
        void invalidId() throws Exception {
            // when
            ResultActions result = mockMvc.perform(get("/api/menus/abc"));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
    }

    @Nested
    @DisplayName("메뉴 수정 PUT /api/menus/{menuId}")
    class Update {

        private static final String PRICE_3500 = """
                {"name": "김밥", "price": 3500, "description": "참기름 향 가득한 기본 김밥"}
                """;

        @Test
        @DisplayName("본인 메뉴를 수정하면 200과 수정된 메뉴를 응답한다 (시나리오 #16)")
        void success() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = updateMenu(owner1, menuId, PRICE_3500);

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.menuId").value(menuId))
                    .andExpect(jsonPath("$.price").value(3500));
        }

        @Test
        @DisplayName("다른 사장님의 메뉴를 수정하면 403 MENU_ACCESS_DENIED (시나리오 #15)")
        void otherOwner() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = updateMenu(owner2, menuId, PRICE_3500);

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("MENU_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("손님이 수정하면 403")
        void customer() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = updateMenu(cust1, menuId, PRICE_3500);

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("가격이 0원이면 400")
        void priceZero() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = updateMenu(owner1, menuId, """
                    {"name": "김밥", "price": 0}
                    """);

            // then
            expectInvalidField(result, "price");
        }

        @Test
        @DisplayName("없는 메뉴면 404 MENU_NOT_FOUND")
        void notFound() throws Exception {
            // when
            ResultActions result = updateMenu(owner1, 9999, PRICE_3500);

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("메뉴 삭제 DELETE /api/menus/{menuId}")
    class Delete {

        @Test
        @DisplayName("본인 메뉴를 삭제하면 204 (시나리오 #35)")
        void success() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = deleteMenu(owner1, menuId);

            // then
            result.andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("다른 사장님의 메뉴를 삭제하면 403 MENU_ACCESS_DENIED (시나리오 #34)")
        void otherOwner() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = deleteMenu(owner2, menuId);

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("MENU_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("손님이 삭제하면 403")
        void customer() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            ResultActions result = deleteMenu(cust1, menuId);

            // then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("삭제한 메뉴는 목록에서 빠진다 (시나리오 #36)")
        void deletedMenuNotInList() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);
            deleteMenu(owner1, menuId);

            // when
            ResultActions result = mockMvc.perform(get("/api/menus"));

            // then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("삭제한 메뉴를 단건 조회하면 404 (시나리오 #37)")
        void deletedMenuNotFound() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);
            deleteMenu(owner1, menuId);

            // when
            ResultActions result = mockMvc.perform(get("/api/menus/{menuId}", menuId));

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        }

        @Test
        @DisplayName("이미 삭제한 메뉴를 다시 삭제하면 404")
        void alreadyDeleted() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);
            deleteMenu(owner1, menuId);

            // when
            ResultActions result = deleteMenu(owner1, menuId);

            // then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        }

        @Test
        @DisplayName("삭제해도 행은 남고 삭제 시각만 기록된다")
        void softDelete() throws Exception {
            // given
            long menuId = createMenuId(owner1, KIMBAP);

            // when
            deleteMenu(owner1, menuId);

            // then
            Integer remaining = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM menus WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, menuId);
            assertThat(remaining).isEqualTo(1);
        }
    }

    private ResultActions createMenu(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/menus")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long createMenuId(String token, String body) throws Exception {
        return readId(createMenu(token, body).andReturn().getResponse().getContentAsString(), "$.menuId");
    }

    private ResultActions updateMenu(String token, long menuId, String body) throws Exception {
        return mockMvc.perform(put("/api/menus/{menuId}", menuId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions deleteMenu(String token, long menuId) throws Exception {
        return mockMvc.perform(delete("/api/menus/{menuId}", menuId)
                .header(HttpHeaders.AUTHORIZATION, token));
    }
}
