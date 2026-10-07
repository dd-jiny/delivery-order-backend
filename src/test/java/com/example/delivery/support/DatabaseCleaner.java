package com.example.delivery.support;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 테스트 DB 컨테이너는 테스트 전체가 함께 쓰므로, 다른 테스트가 남긴 데이터가 섞이지 않게 테스트 전에 모든 테이블을 비운다.
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void truncateAllTables(JdbcTemplate jdbcTemplate) {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        if (tables.isEmpty()) {
            return;
        }
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
    }
}
