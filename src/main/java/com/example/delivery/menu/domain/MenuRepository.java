package com.example.delivery.menu.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * 메뉴 저장소. domain은 "무엇이 필요한지"만 정하고, DB 접근 구현은 infrastructure(MenuRepositoryImpl)가 맡는다.
 * Page는 DB 기술이 아닌 페이징 결과 표현(Spring Data Commons)이라 그대로 쓴다.
 */
public interface MenuRepository {

    Menu save(Menu menu);

    /** 삭제된 메뉴는 없는 것으로 본다. */
    Optional<Menu> findByIdExcludingDeleted(Long id);

    /** 삭제된 메뉴를 뺀 목록을 최신 등록순으로 조회한다. */
    Page<Menu> findAllExcludingDeleted(int page, int size);
}
