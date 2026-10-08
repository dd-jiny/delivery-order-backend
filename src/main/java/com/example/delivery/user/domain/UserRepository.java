package com.example.delivery.user.domain;

import java.util.Optional;

/**
 * 회원 저장소. domain은 "무엇이 필요한지"만 정하고, DB 접근 구현은 infrastructure(UserRepositoryImpl)가 맡는다.
 */
public interface UserRepository {

    User save(User user);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    /** 조회 쿼리 없이 참조만 만든다. 토큰으로 인증된 회원처럼 존재가 보장된 경우에만 쓴다. */
    User getReferenceById(Long id);
}
