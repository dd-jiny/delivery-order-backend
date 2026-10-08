package com.example.delivery.menu.infrastructure;

import com.example.delivery.menu.domain.Menu;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuJpaRepository extends JpaRepository<Menu, Long> {

    Page<Menu> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Menu> findByIdAndDeletedAtIsNull(Long id);
}
