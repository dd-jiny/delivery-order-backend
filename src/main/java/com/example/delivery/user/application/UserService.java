package com.example.delivery.user.application;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.global.infrastructure.security.JwtProvider;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserRepository;
import com.example.delivery.user.presentation.LoginRequest;
import com.example.delivery.user.presentation.LoginResponse;
import com.example.delivery.user.presentation.SignupRequest;
import com.example.delivery.user.presentation.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 중복 검사를 통과해도 동시 가입이면 DB UNIQUE 제약에 걸린다. 그 경우도 같은 409로 응답한다.
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        User user = User.create(request.username(), passwordEncoder.encode(request.password()), request.role());
        try {
            return UserResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    /**
     * 회원 없음·탈퇴 회원·비밀번호 불일치를 구분하지 않고 같은 401로 응답해 아이디 존재 여부가 드러나지 않게 한다.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .filter(User::isActive)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        String token = jwtProvider.createToken(user.getId(), user.getUsername(), user.getRole());
        return LoginResponse.bearer(token, jwtProvider.getExpirationSeconds());
    }
}
