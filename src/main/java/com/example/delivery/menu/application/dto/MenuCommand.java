package com.example.delivery.menu.application.dto;

/**
 * 메뉴 등록·수정 입력. 형식 검증은 presentation(MenuRequest)에서 끝난 값이다.
 */
public record MenuCommand(String name, Long price, String description) {
}
