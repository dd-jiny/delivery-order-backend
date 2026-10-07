package com.example.delivery.menu.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Page<Menu> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Menu> findByIdAndDeletedAtIsNull(Long id);
}
