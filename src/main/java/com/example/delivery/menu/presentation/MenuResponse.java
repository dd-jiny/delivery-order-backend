package com.example.delivery.menu.presentation;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record MenuResponse(
        Long menuId,
        Long ownerId,
        String name,
        Long price,
        String description,
        MenuStatus status,
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
                menu.getStatus(),
                menu.getCreatedAt(),
                menu.getUpdatedAt());
    }
}
