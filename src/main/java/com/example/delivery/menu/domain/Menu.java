package com.example.delivery.menu.domain;

import com.example.delivery.global.domain.BaseEntity;
import com.example.delivery.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuStatus status;

    private LocalDateTime deletedAt;

    private Menu(User owner, String name, Long price, String description) {
        this.owner = owner;
        this.name = name;
        this.price = price;
        this.description = description;
        this.status = MenuStatus.ON_SALE;
    }

    public static Menu create(User owner, String name, Long price, String description) {
        return new Menu(owner, name, price, description);
    }

    /**
     * 전체 교체(PUT). 설명을 null로 받으면 설명이 비워진다.
     */
    public void update(String name, Long price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    /**
     * Soft Delete. 행은 남기고 삭제 시각만 기록한다. 시각은 호출하는 쪽(Clock)에서 받는다.
     */
    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    public boolean isOnSale() {
        return status == MenuStatus.ON_SALE;
    }
}
