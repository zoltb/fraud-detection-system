package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.FraudCheckResult;
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

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FraudDetectionServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock // For opsForValue - for chained call because Mocks give back only null
    ValueOperations<String, String> valueOperations;
    @Mock
    private FraudAppConfig fraudAppConfig;

    @InjectMocks
    private FraudDetectionService service;

    private Transaction tx;

    @BeforeEach
    void setUp() {
        tx = Transaction.builder()
                .userId(1L)
                .amount(new BigDecimal("100"))
                .build();

        // Setup Config mock
        var detection = new FraudAppConfig.Detection();
        // Card Test config
        var cardTest = new FraudAppConfig.Detection.CardTest();
        cardTest.setAmountLimit(new BigDecimal("200"));
        cardTest.setCountLimit(2);
        cardTest.setDurationMinutes(10);
        detection.setCardTest(cardTest);

        // Velocity config
        var velocity = new FraudAppConfig.Detection.Velocity();
        velocity.setCountLimit(5);
        velocity.setDurationMinutes(10);
        detection.setVelocity(velocity);

        lenient().when(fraudAppConfig.getDetection()).thenReturn(detection);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Transaction without fraud")
    void check_ShouldReturnNoFraud() {
        // GIVEN
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertFalse(result.isFraud());
        assertNull(result.fraudType());
    }

    @Test
    @DisplayName("Transaction fraud - Card Testing")
    void check_LotOfLittleAmountShouldReturnCardTestingFraud() {
        // GIVEN
        // Limit (100 < 200)
        // Increment gives 3 (the limit is 2)
        when(valueOperations.increment(contains("card_test:"))).thenReturn(3L);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertTrue(result.isFraud());
        assertEquals("CARD_TESTING", result.fraudType());
    }

    @Test
    @DisplayName("Transaction fraud - Velocity")
    void check_LotOfBiggerAmountShouldReturnVelocityFraud() {
        // GIVEN
        // Az összeg most legyen magasabb, hogy a Card Test ágat átugorjuk
        tx.setAmount(new BigDecimal("500"));
        when(valueOperations.increment(contains("velocity:"))).thenReturn(10L);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertTrue(result.isFraud());
        assertEquals("VELOCITY", result.fraudType());
    }

    @Test
    @DisplayName("Expire! If counter 1, Redis test")
    void check_ShouldSetExpiry_WhenCountIsOne() {
        // GIVEN
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // WHEN
        service.check(tx);

        // THEN
        // Ellenőrizzük, hogy meghívódott-e a redisTemplate.expire()
        verify(stringRedisTemplate, atLeastOnce()).expire(anyString(), any(Duration.class));
    }
}
