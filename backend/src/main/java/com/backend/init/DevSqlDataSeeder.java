package com.backend.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class DevSqlDataSeeder {

    private static final int EXPECTED_ACTIVITY_COUNT = 4;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    @Value("${app.seed.sql-location:classpath:db/seed/dev-data.sql}")
    private String sqlLocation;

    @Bean
    @ConditionalOnProperty(name = "app.seed.dev-data", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedDevDataFromSql() {
        return args -> {
            Integer activityCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM activity", Integer.class);
            if (activityCount != null && activityCount >= EXPECTED_ACTIVITY_COUNT) {
                log.info("演示数据已就绪（活动 {} 条），跳过种子脚本", activityCount);
                return;
            }

            Resource script = resourceLoader.getResource(sqlLocation);
            if (!script.exists()) {
                log.warn("未找到演示数据脚本: {}", sqlLocation);
                return;
            }

            log.info("正在导入演示数据: {}（当前活动 {} 条）", sqlLocation, activityCount);
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, script);
            }
            Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activity", Integer.class);
            log.info("演示数据导入完成：活动 {} 条，用户 {} 条，报名 {} 条",
                    after,
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class),
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM registration", Integer.class));
        };
    }
}
