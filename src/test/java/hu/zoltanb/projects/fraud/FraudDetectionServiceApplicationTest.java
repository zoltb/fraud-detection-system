package hu.zoltanb.projects.fraud;

import hu.zoltanb.projects.fraud.service.RedisAndDbCleaner;
import hu.zoltanb.projects.fraud.service.TransactionConsumer;
import hu.zoltanb.projects.fraud.service.TransactionProducer;
import hu.zoltanb.projects.fraud.transactiongenerator.TransactionGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest()
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class
})
class FraudDetectionServiceApplicationTest {

    /**
     * These components are mocked to ensure the Spring ApplicationContext can load
     * successfully during integration tests.
     *
     * Without these @MockBeans, the context initialization would fail because:
     * 1. TransactionProducer/Consumer: Require a running messaging broker (e.g., Kafka).
     * 2. TransactionGeneratorService: Triggers external dependencies or scheduled tasks.
     * 3. RedisConnectionFactory & StringRedisTemplate: Required by FraudDetectionService
     * via constructor injection. Without these, an UnsatisfiedDependencyException occurs.
     * 4. RedisAndDbCleaner: Prevent @PostConstruct from executing database truncates
     * and Redis flushes during startup.
     *
     * Mocking these infrastructure-heavy beans allows for a fast and isolated
     * "smoke test" of the application configuration.
     */

    @MockBean
    private StringRedisTemplate stringRedisTemplate;
    @MockBean
    private TransactionProducer transactionProducer;
    @MockBean
    private TransactionConsumer transactionConsumer;
    @MockBean
    private TransactionGeneratorService transactionGeneratorService;
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockBean
    private RedisAndDbCleaner redisAndDbCleaner;


    //Smoke test -> checking only the Spring context
    @Test
    void contextLoads() {
    }

}
