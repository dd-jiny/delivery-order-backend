package com.example.delivery.user.presentation;

import com.example.delivery.user.application.dto.SignupCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 상한 20자는 BCrypt 72바이트 제한을 넘지 않기 위한 것이다.
 * 역할은 domain enum을 모르도록 문자열로 받고, enum 변환은 application(UserFacade)이 한다.
 */
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        String password,

        @NotBlank(message = "역할은 필수입니다.")
        @Pattern(regexp = "CUSTOMER|OWNER", message = "역할은 CUSTOMER 또는 OWNER여야 합니다.")
        String role
) {

    public SignupCommand toCommand() {
        return new SignupCommand(username, password, role);
    }
}
