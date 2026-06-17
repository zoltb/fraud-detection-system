package hu.zoltanb.projects.fraud.service.detector;
import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.Transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VelocityDetectorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FraudAppConfig config;

    @InjectMocks
    private VelocityDetector detector;

    private FraudAppConfig.Detection detection;
    private FraudAppConfig.Detection.Velocity velocityConfig;

    @BeforeEach
    void setUp() {
        detection = new FraudAppConfig.Detection();
        velocityConfig = new FraudAppConfig.Detection.Velocity();
        velocityConfig.setCountLimit(5);
        velocityConfig.setDurationMinutes(10);
        detection.setVelocity(velocityConfig);

        lenient().when(config.getDetection()).thenReturn(detection);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Sets expiry, if the counter is one")
    void shouldSetExpiryWhenCountIsOne() {
        Transaction tx = Transaction.builder().userId(1L).build();
        when(valueOperations.increment(anyString())).thenReturn(1L);
        List<String> fraudTypes = new ArrayList<>();

        detector.check(tx, fraudTypes);

        verify(redisTemplate).expire(eq("velocity:1"), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("Adds VELOCITY if counter exceeds the limit.")
    void shouldFlagWhenLimitExceeded() {
        Transaction tx = Transaction.builder().userId(1L).build();
        // The limit is 5, the 6 is fraud
        when(valueOperations.increment(anyString())).thenReturn(6L);
        List<String> fraudTypes = new ArrayList<>();

        detector.check(tx, fraudTypes);

        assertTrue(fraudTypes.contains("VELOCITY"));
    }
}