package com.example.delivery.menu.domain;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.user.domain.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * 메뉴 도메인 서비스. 조회·존재 확인(404)·소유 확인(403)·저장처럼 Repository가 필요한 메뉴 단위 작업을 맡고,
 * 메뉴·주문 Facade가 재사용한다. 규칙 자체(수정·삭제·소유 판단)는 Menu 엔티티에 있다. 트랜잭션은 Facade가 연다.
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final Clock clock;

    public Menu register(User owner, String name, Long price, String description) {
        return menuRepository.save(Menu.create(owner, name, price, description));
    }

    public Page<Menu> getMenus(int page, int size) {
        return menuRepository.findAllExcludingDeleted(page, size);
    }

    /** 삭제된 메뉴도 없는 메뉴(404)로 본다. */
    public Menu getMenu(Long menuId) {
        return menuRepository.findByIdExcludingDeleted(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
    }

    public Menu update(Long ownerId, Long menuId, String name, Long price, String description) {
        Menu menu = getOwnedMenu(ownerId, menuId);
        menu.update(name, price, description);
        return menu;
    }

    public void delete(Long ownerId, Long menuId) {
        Menu menu = getOwnedMenu(ownerId, menuId);
        menu.delete(LocalDateTime.now(clock));
    }

    /** 존재 확인(404)을 먼저 하고 소유 확인(403)을 한다 (01 D-01). */
    private Menu getOwnedMenu(Long ownerId, Long menuId) {
        Menu menu = getMenu(menuId);
        if (!menu.isOwnedBy(ownerId)) {
            throw new BusinessException(ErrorCode.MENU_ACCESS_DENIED);
        }
        return menu;
    }
}
