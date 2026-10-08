package com.example.delivery.user.application.dto;

/**
 * 로그인 입력. 형식 검증은 presentation(LoginRequest)에서 끝난 값이다.
 */
public record LoginCommand(String username, String password) {
}
