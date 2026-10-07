package com.example.delivery.global.infrastructure.security;

import com.example.delivery.user.domain.UserRole;

/**
 * 인증된 요청자. JWT 클레임만으로 만들며 DB를 조회하지 않는다.
 * Controller에서 @AuthenticationPrincipal로 받아 userId·role을 Service에 넘긴다.
 */
public record AuthUser(Long userId, String username, UserRole role) {
}
