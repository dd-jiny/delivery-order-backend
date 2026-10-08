package com.example.delivery.user.application.dto;

import com.example.delivery.user.domain.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 회원 응답. 비밀번호는 담지 않는다. domain enum은 밖으로 내보내지 않고 문자열로 바꾼다.
 */
public record UserResponse(
        Long userId,
        String username,
        String role,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getCreatedAt());
    }
}
