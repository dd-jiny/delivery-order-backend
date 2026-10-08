package com.example.delivery.menu.infrastructure;

import com.example.delivery.menu.domain.Menu;
import com.example.delivery.menu.domain.MenuRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MenuRepositoryImpl implements MenuRepository {

    /** 최신 등록순. 같은 시각이면 나중에 저장된(id가 큰) 메뉴가 먼저 온다. */
    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final MenuJpaRepository menuJpaRepository;

    @Override
    public Menu save(Menu menu) {
        return menuJpaRepository.save(menu);
    }

    @Override
    public Optional<Menu> findByIdExcludingDeleted(Long id) {
        return menuJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Menu> findAllExcludingDeleted(int page, int size) {
        return menuJpaRepository.findAllByDeletedAtIsNull(PageRequest.of(page, size, LATEST_FIRST));
    }
}
