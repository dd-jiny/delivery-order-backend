package com.example.delivery.menu.application;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.global.presentation.PageResponse;
import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuRepository;
import com.example.delivery.menu.presentation.MenuRequest;
import com.example.delivery.menu.presentation.MenuResponse;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuService {

    /** 목록은 최신 등록순 고정. 같은 시각이면 나중에 저장된(id가 큰) 메뉴가 먼저 온다. */
    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * 토큰이 유효하면 회원은 존재하므로 조회 없이 참조만 만든다.
     */
    @Transactional
    public MenuResponse create(Long ownerId, MenuRequest request) {
        User owner = userRepository.getReferenceById(ownerId);
        Menu menu = Menu.create(owner, request.name(), request.price(), request.description());
        return MenuResponse.from(menuRepository.save(menu));
    }

    @Transactional(readOnly = true)
    public PageResponse<MenuResponse> getMenus(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, LATEST_FIRST);
        return PageResponse.from(menuRepository.findAllByDeletedAtIsNull(pageRequest).map(MenuResponse::from));
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenu(Long menuId) {
        return MenuResponse.from(findMenu(menuId));
    }

    @Transactional
    public MenuResponse update(Long ownerId, Long menuId, MenuRequest request) {
        Menu menu = findOwnedMenu(ownerId, menuId);
        menu.update(request.name(), request.price(), request.description());
        return MenuResponse.from(menu);
    }

    @Transactional
    public void delete(Long ownerId, Long menuId) {
        Menu menu = findOwnedMenu(ownerId, menuId);
        menu.delete(LocalDateTime.now(clock));
    }

    private Menu findMenu(Long menuId) {
        return menuRepository.findByIdAndDeletedAtIsNull(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
    }

    /** 존재 확인(404)을 먼저 하고 소유 확인(403)을 한다 (01 D-01). */
    private Menu findOwnedMenu(Long ownerId, Long menuId) {
        Menu menu = findMenu(menuId);
        if (!menu.isOwnedBy(ownerId)) {
            throw new BusinessException(ErrorCode.MENU_ACCESS_DENIED);
        }
        return menu;
    }
}
