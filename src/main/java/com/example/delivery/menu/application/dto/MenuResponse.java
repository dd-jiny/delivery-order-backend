package com.example.delivery.menu.application.dto;

import com.example.delivery.menu.domain.Menu;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 메뉴 응답. domain enum(MenuStatus)은 밖으로 내보내지 않고 문자열로 바꾼다.
 */
public record MenuResponse(
        Long menuId,
        Long ownerId,
        String name,
        Long price,
        String description,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {

    /** owner는 지연 로딩 프록시여도 getId()는 조회 없이 읽힌다. */
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getOwner().getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getDescription(),
                menu.getStatus().name(),
                menu.getCreatedAt(),
                menu.getUpdatedAt());
    }
}
