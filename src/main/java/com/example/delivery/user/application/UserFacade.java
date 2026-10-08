package com.example.delivery.user.application;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.JwtProvider;
import com.example.delivery.user.application.dto.LoginCommand;
import com.example.delivery.user.application.dto.LoginResponse;
import com.example.delivery.user.application.dto.SignupCommand;
import com.example.delivery.user.application.dto.UserResponse;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserRole;
import com.example.delivery.user.domain.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 유스케이스 흐름을 조율한다. 비밀번호 암호화·대조와 토큰 발급(기술)을 도메인 서비스와 엮는다.
 */
@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 역할 문자열은 presentation의 @Pattern으로 검증된 값이라 enum 변환이 실패하지 않는다.
     */
    @Transactional
    public UserResponse signup(SignupCommand command) {
        UserRole role = UserRole.valueOf(command.role());
        User user = userService.register(command.username(), passwordEncoder.encode(command.password()), role);
        return UserResponse.from(user);
    }

    /**
     * 회원 없음·탈퇴(도메인 서비스)와 비밀번호 불일치를 모두 같은 401로 응답한다.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginCommand command) {
        User user = userService.getLoginUser(command.username());
        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtProvider.createToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.bearer(token, jwtProvider.getExpirationSeconds());
    }
}
