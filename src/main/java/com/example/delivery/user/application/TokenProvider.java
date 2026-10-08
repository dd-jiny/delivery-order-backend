package com.example.delivery.user.application;

import com.example.delivery.user.domain.UserRole;

/**
 * 로그인에 필요한 토큰 발급 기능. 쓰는 쪽(application)이 필요한 모양을 정하고, 기술 구현(JWT)은 infrastructure가 맡는다 (04 D-33).
 * UserFacade는 토큰이 JWT인지 모른다.
 */
public interface TokenProvider {

    String createToken(Long userId, String username, UserRole role);

    long getExpirationSeconds();
}
