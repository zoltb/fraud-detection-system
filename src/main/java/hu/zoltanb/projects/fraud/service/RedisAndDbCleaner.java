package hu.zoltanb.projects.fraud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class RedisAndDbCleaner {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private Sleeper sleeper = new Sleeper(); // Skip using Spring injection

    // Now I can mock it in test
    public void setSleeper(Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    // Instead of @PostConstruct because ApplicationReadyEvent: it will run when app started to run
    @EventListener(ApplicationReadyEvent.class)
    public void cleanOnStart() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.info("Redis cache is empty!");

        log.info("Cleaning up PostgreSQL transactions table...");
        jdbcTemplate.execute("TRUNCATE TABLE transactions RESTART IDENTITY CASCADE");
        log.info("PostgreSQL table is empty and IDs are reset!");

        try {
            log.info("===> CLEANUP DONE. Waiting 5 seconds for visual check...");
            sleeper.sleep(5000);
            log.info("===> Resume processing...");
        } catch (InterruptedException e) {
            log.error("Sleep interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();

        }

    }

    @Component
    public static class Sleeper {
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    }

}