package com.example.delivery.user.presentation;

import com.example.delivery.user.domain.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.delivery.user.domain.UserRole;
import java.time.LocalDateTime;

/**
 * 회원 응답. 비밀번호는 담지 않는다.
 */
public record UserResponse(
        Long userId,
        String username,
        UserRole role,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
