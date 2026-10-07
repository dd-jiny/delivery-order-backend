package com.example.delivery.menu.presentation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 메뉴 등록·수정 공통. 수정은 전체 교체(PUT)라 설명을 빼면 설명이 비워진다.
 */
public record MenuRequest(
        @NotBlank(message = "메뉴 이름은 필수입니다.")
        @Size(max = 100, message = "메뉴 이름은 100자 이하여야 합니다.")
        String name,

        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 1, message = "가격은 1원 이상이어야 합니다.")
        Long price,

        @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
        String description
) {
}
