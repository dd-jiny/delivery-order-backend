package com.example.delivery.user.domain;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 회원 도메인 서비스. Repository가 필요한 회원 단위 작업(중복 확인, 조회, 저장)을 맡고,
 * 여러 Facade가 재사용한다. 트랜잭션은 호출하는 Facade가 연다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 동시 가입으로 중복 검사를 통과해도 저장 시 UserRepository가 같은 409로 바꿔 던진다.
     */
    public User register(String username, String encodedPassword, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        return userRepository.save(User.create(username, encodedPassword, role));
    }

    /**
     * 로그인할 수 있는 회원을 찾는다. 없는 아이디와 탈퇴 회원을 구분하지 않고 같은 401로 거절한다.
     */
    public User getLoginUser(String username) {
        return userRepository.findByUsername(username)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    }

    /**
     * 조회 없이 참조만 만든다. 토큰으로 인증된 회원처럼 존재가 보장된 경우에만 쓴다.
     */
    public User getReference(Long userId) {
        return userRepository.getReferenceById(userId);
    }
}
