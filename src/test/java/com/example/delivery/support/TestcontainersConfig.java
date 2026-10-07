package com.example.delivery.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 테스트용 PostgreSQL 컨테이너. @ServiceConnection이 접속 정보를 자동으로 연결하므로 .env가 필요 없다.
 * 컨테이너를 static으로 하나만 만들어, 테스트 컨텍스트가 여러 개(E2E, @DataJpaTest)여도 테스트 전체에서 재사용한다.
 * 종료는 Testcontainers(Ryuk)가 JVM이 끝날 때 처리하므로 컨텍스트가 닫힐 때 멈추지 않게 한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Bean(destroyMethod = "")
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
