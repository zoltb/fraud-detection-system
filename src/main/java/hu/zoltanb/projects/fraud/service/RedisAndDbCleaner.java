package hu.zoltanb.projects.fraud.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisAndDbCleaner {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    @ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true")
    public void cleanOnStart() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.info("Redis cache is empty!");

        log.info("Cleaning up PostgreSQL transactions table...");
        jdbcTemplate.execute("TRUNCATE TABLE transactions RESTART IDENTITY CASCADE");
        log.info("PostgreSQL table is empty and IDs are reset!");

        try {
            log.info("===> CLEANUP DONE. Waiting 10 seconds for visual check...");
            Thread.sleep(10000);
            log.info("===> Resume processing...");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Sleep interrupted", e);
        }

    }

}