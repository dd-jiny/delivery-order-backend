package com.example.delivery.menu.presentation;

import com.example.delivery.global.infrastructure.security.AuthUser;
import com.example.delivery.global.presentation.PageResponse;
import com.example.delivery.menu.application.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuResponse create(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody MenuRequest request) {
        return menuService.create(authUser.userId(), request);
    }

    /**
     * page·size만 사용한다. 기본 크기 10, 최대 50은 spring.data.web.pageable 설정이 보정하고, 정렬은 Service가 최신 등록순으로 고정한다.
     */
    @GetMapping
    public PageResponse<MenuResponse> getMenus(Pageable pageable) {
        return menuService.getMenus(pageable.getPageNumber(), pageable.getPageSize());
    }

    @GetMapping("/{menuId}")
    public MenuResponse getMenu(@PathVariable Long menuId) {
        return menuService.getMenu(menuId);
    }

    @PutMapping("/{menuId}")
    public MenuResponse update(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long menuId,
            @Valid @RequestBody MenuRequest request
    ) {
        return menuService.update(authUser.userId(), menuId, request);
    }

    @DeleteMapping("/{menuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long menuId) {
        menuService.delete(authUser.userId(), menuId);
    }
}
