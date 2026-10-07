package com.example.delivery.user.presentation;

import com.example.delivery.user.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 상한 20자는 BCrypt 72바이트 제한을 넘지 않기 위한 것이다.
 */
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        String password,

        @NotNull(message = "역할은 필수입니다.")
        UserRole role
) {
}
