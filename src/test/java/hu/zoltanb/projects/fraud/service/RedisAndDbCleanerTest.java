package hu.zoltanb.projects.fraud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisAndDbCleanerTest {
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private
    RedisConnectionFactory connectionFactory;
    @Mock
    private RedisConnection connection;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private RedisAndDbCleaner.Sleeper sleeper;

    @InjectMocks
    private RedisAndDbCleaner cleaner;

    @BeforeEach
    void setUp() {
        // Lenient because some test will be shorter and won't run everything.
        lenient().when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        lenient().when(connectionFactory.getConnection()).thenReturn(connection);
        // set sleeper by hand
        cleaner.setSleeper(sleeper);
    }

    @Test
    @DisplayName("Checking successful run: All commands executed and waiting for 10 seconds.")
    void cleanOnStart_Success() throws InterruptedException{
        // GIVEN Mocking the Redis call
        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);

        // WHEN Run
        cleaner.cleanOnStart();

        // THEN Checking flushAll
        then(connection).should(times(1)).flushAll();
        then(jdbcTemplate).should().execute(contains("TRUNCATE"));
        then(sleeper).should().sleep(5000);
        then(jdbcTemplate).should(times(1))
                .execute("TRUNCATE TABLE transactions RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("InterruptedException test: checking the catch")
    void cleanOnStart_HandlesInterrupt() throws Exception { // Because of the checked exception should be handled
        // GIVEN: Sleeper throws exception
        willThrow(new InterruptedException())
                .given(sleeper).sleep(anyLong());

        // WHEN
        cleaner.cleanOnStart();

        // THEN
        // Checking catch where the Thread will be interrupted again.
        assertTrue(Thread.currentThread().isInterrupted());
        // Codecov will see the run of the catch!

        // Clean the thread
        Thread.interrupted();
    }
}
