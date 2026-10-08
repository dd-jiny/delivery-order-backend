package com.example.delivery.global.application.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이지 응답. Spring의 Page를 그대로 직렬화하면 JSON 구조가 내부 구현에 묶이므로 필요한 필드만 담는다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
