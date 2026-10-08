package com.example.delivery.menu.application;

import com.example.delivery.global.application.dto.PageResponse;
import com.example.delivery.menu.application.dto.MenuCommand;
import com.example.delivery.menu.application.dto.MenuResponse;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuService;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 유스케이스 흐름을 조율한다. 트랜잭션을 열고, 도메인 서비스를 호출하고, 엔티티를 응답 DTO로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class MenuFacade {

    private final MenuService menuService;
    private final UserService userService;

    @Transactional
    public MenuResponse create(Long ownerId, MenuCommand command) {
        User owner = userService.getReference(ownerId);
        Menu menu = menuService.register(owner, command.name(), command.price(), command.description());
        return MenuResponse.from(menu);
    }

    @Transactional(readOnly = true)
    public PageResponse<MenuResponse> getMenus(int page, int size) {
        return PageResponse.from(menuService.getMenus(page, size).map(MenuResponse::from));
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenu(Long menuId) {
        return MenuResponse.from(menuService.getMenu(menuId));
    }

    @Transactional
    public MenuResponse update(Long ownerId, Long menuId, MenuCommand command) {
        Menu menu = menuService.update(ownerId, menuId, command.name(), command.price(), command.description());
        return MenuResponse.from(menu);
    }

    @Transactional
    public void delete(Long ownerId, Long menuId) {
        menuService.delete(ownerId, menuId);
    }
}
