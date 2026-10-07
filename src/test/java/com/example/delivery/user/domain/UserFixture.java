package com.example.delivery.user.domain;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * 테스트용 회원 생성. 테스트에 중요한 값만 인자로 받는다.
 */
public class UserFixture {

    public static final String ENCODED_PASSWORD = "$2a$10$encoded";

    public static User customer() {
        return User.create("cust1", ENCODED_PASSWORD, UserRole.CUSTOMER);
    }

    public static User owner() {
        return User.create("owner1", ENCODED_PASSWORD, UserRole.OWNER);
    }

    public static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** 탈퇴 API가 없으므로 상태를 직접 바꾼다. */
    public static User withdrawn(User user) {
        ReflectionTestUtils.setField(user, "status", UserStatus.WITHDRAWN);
        return user;
    }
}
