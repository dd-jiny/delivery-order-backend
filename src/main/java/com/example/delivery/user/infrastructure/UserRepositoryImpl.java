package com.example.delivery.user.infrastructure;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.user.domain.User;
import com.example.delivery.user.domain.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    /**
     * 중복 검사를 통과해도 동시 가입이면 DB UNIQUE 제약에 걸린다. users의 UNIQUE 제약은 username 하나뿐이라
     * 제약 위반을 아이디 중복(409)으로 바꾼다. domain이 DB 예외를 모르게 하려고 여기서 변환한다.
     */
    @Override
    public User save(User user) {
        try {
            return userJpaRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username);
    }

    @Override
    public User getReferenceById(Long id) {
        return userJpaRepository.getReferenceById(id);
    }
}
