package com.example.delivery.user.domain;

import com.example.delivery.global.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false, length = 60)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private LocalDateTime withdrawnAt;

    private User(String username, String encodedPassword, UserRole role) {
        this.username = username;
        this.password = encodedPassword;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 이미 암호화된 비밀번호를 받는다. 암호화는 Service가 PasswordEncoder로 한다.
     */
    public static User create(String username, String encodedPassword, UserRole role) {
        return new User(username, encodedPassword, role);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isOwner() {
        return role == UserRole.OWNER;
    }
}
