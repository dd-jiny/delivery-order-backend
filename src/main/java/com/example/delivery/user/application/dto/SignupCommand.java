package com.example.delivery.user.application.dto;

/**
 * 회원가입 입력. 형식 검증은 presentation(SignupRequest)에서 끝난 값이다.
 * 역할은 문자열로 받아 UserFacade가 domain enum으로 바꾼다.
 */
public record SignupCommand(String username, String password, String role) {
}
