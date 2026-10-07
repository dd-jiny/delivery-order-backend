package com.example.delivery.global.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각을 Clock 빈으로 주입받게 해서, 테스트에서 시각을 고정할 수 있게 한다 (JWT 만료, 메뉴 삭제 시각 등).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
