package com.example.delivery.support;

import com.example.delivery.global.infrastructure.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository 테스트 공통 부모. 내장 DB 대신 Testcontainers PostgreSQL을 쓰고,
 * 생성·수정 시각 검증을 위해 JPA Auditing 설정을 함께 불러온다.
 * 테스트 자체는 롤백되지만, 같은 컨테이너를 쓰는 E2E 테스트가 남긴 데이터가 있을 수 있어 시작 전에 테이블을 비운다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, JpaAuditingConfig.class})
public abstract class RepositoryTestSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        DatabaseCleaner.truncateAllTables(jdbcTemplate);
    }
}
