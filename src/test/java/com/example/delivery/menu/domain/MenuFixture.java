package com.example.delivery.menu.domain;

import com.example.delivery.user.domain.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트용 메뉴 생성. 테스트에 중요한 값만 인자로 받는다.
 */
public class MenuFixture {

    public static Menu kimbap(User owner) {
        return Menu.create(owner, "김밥", 3000L, "참기름 향 가득한 기본 김밥");
    }

    public static Menu menu(User owner, String name) {
        return Menu.create(owner, name, 3000L, null);
    }

    public static Menu withId(Menu menu, Long id) {
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

    /** 판매 상태 변경 API가 없으므로 상태를 직접 바꾼다. */
    public static Menu soldOut(Menu menu) {
        ReflectionTestUtils.setField(menu, "status", MenuStatus.SOLD_OUT);
        return menu;
    }
}
