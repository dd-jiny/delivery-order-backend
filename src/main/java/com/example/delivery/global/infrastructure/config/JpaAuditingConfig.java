package com.example.delivery.global.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 메인 클래스가 아닌 별도 설정 클래스에 두어, JPA를 띄우지 않는 슬라이스 테스트(@WebMvcTest 등)에서 Auditing 설정 오류가 나지 않게 한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
