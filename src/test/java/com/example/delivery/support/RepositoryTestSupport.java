package com.example.delivery.support;

import com.example.delivery.global.infrastructure.config.JpaAuditingConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository 테스트 공통 부모. 내장 DB 대신 Testcontainers PostgreSQL을 쓰고,
 * 생성·수정 시각 검증을 위해 JPA Auditing 설정을 함께 불러온다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, JpaAuditingConfig.class})
public abstract class RepositoryTestSupport {
}
