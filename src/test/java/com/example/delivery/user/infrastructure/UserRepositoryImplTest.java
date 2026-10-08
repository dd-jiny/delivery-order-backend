package com.example.delivery.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.delivery.global.domain.exception.BusinessException;
import com.example.delivery.global.domain.exception.ErrorCode;
import com.example.delivery.support.RepositoryTestSupport;
import com.example.delivery.user.domain.UserFixture;
import com.example.delivery.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * domain의 UserRepository 약속을 구현이 지키는지 확인한다.
 */
@Import(UserRepositoryImpl.class)
class UserRepositoryImplTest extends RepositoryTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("동시 가입으로 DB UNIQUE 제약에 걸리면 409 DUPLICATE_USERNAME으로 바꿔 던진다")
    void save_duplicateUsername() {
        // given
        userRepository.save(UserFixture.owner());

        // when & then
        assertThatThrownBy(() -> userRepository.save(UserFixture.owner()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_USERNAME);
    }
}
