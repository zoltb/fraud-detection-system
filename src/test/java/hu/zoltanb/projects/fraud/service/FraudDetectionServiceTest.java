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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
        given(valueOperations.increment(startsWith("card_test:"))).willReturn(1L);
        given(valueOperations.increment(startsWith("velocity:"))).willReturn(1L);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertFalse(result.isFraud());
        assertTrue(result.fraudTypes().isEmpty());

    }

    @Test
    @DisplayName("Transaction fraud - Card Testing")
    void check_LotOfLittleAmountShouldReturnCardTestingFraud() {
        // GIVEN
        // Limit (100 < 200)
        // Increment gives 3 (the limit is 2)
        given(valueOperations.increment(startsWith("card_test:"))).willReturn(3L);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertTrue(result.isFraud());
        assertTrue(result.fraudTypes().contains("CARD_TESTING"));
    }

    @Test
    @DisplayName("Transaction fraud - Velocity")
    void check_LotOfBiggerAmountShouldReturnVelocityFraud() {
        // GIVEN
        // Az összeg most legyen magasabb, hogy a Card Test ágat átugorjuk
        tx.setAmount(new BigDecimal("500"));
        given(valueOperations.increment(startsWith("velocity:"))).willReturn(10L);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertTrue(result.isFraud());
        assertTrue(result.fraudTypes().contains("VELOCITY"));
    }

    @Test
    @DisplayName("Expire! If counter 1, Redis test")
    void check_ShouldSetExpiry_WhenCountIsOne() {
        // GIVEN
        given(valueOperations.increment(startsWith("card_test:"))).willReturn(1L);
        given(valueOperations.increment(startsWith("velocity:"))).willReturn(1L);

        // WHEN
        service.check(tx);

        // THEN
        // Ellenőrizzük, hogy meghívódott-e a redisTemplate.expire()
        verify(stringRedisTemplate).expire(eq("card_test:1"), eq(Duration.ofMinutes(10)));
        verify(stringRedisTemplate).expire(eq("velocity:1"), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("Should handle null only for Velocity check")
    void check_ShouldHandleVelocityNullWhenCardTestIsOk() {
        // GIVEN we have value for card testing but for velocity we receive null
        given(valueOperations.increment(startsWith("card_test:"))).willReturn(null);
        given(valueOperations.increment(startsWith("velocity:"))).willReturn(null);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertFalse(result.isFraud());
    }

    @Test
    @DisplayName("Should not call expire if counter is not 1")
    void check_ShouldNotExpireIfCounterIsTwo() {
        // GIVEN
        given(valueOperations.increment(startsWith("card_test:"))).willReturn(2L);
        given(valueOperations.increment(startsWith("velocity:"))).willReturn(null);

        // WHEN
        service.check(tx);

        // THEN
        // we didn't call expire because we didn't receive 1
        then(stringRedisTemplate).should(never()).expire(startsWith("card_test:"), any(Duration.class));
        then(stringRedisTemplate).should(never()).expire(startsWith("velocity:"), any(Duration.class));
    }
    
    @Test
    @DisplayName("Should skip card testing if amount is above limit")
    void check_ShouldSkipCardTesting_WhenAmountIsLarge() {
        // GIVEN
        tx = Transaction.builder().userId(1L).amount(new BigDecimal("500")).build();
        given(valueOperations.increment(startsWith("velocity:"))).willReturn(1L);

        // WHEN
        service.check(tx);

        // THEN
        verify(valueOperations, never()).increment(startsWith("card_test:"));
        verify(valueOperations).increment(startsWith("velocity:"));
    }

    @Test
    @DisplayName("Should handle null Redis for Card testing but continue to Velocity")
    void check_ShouldHandleNullCardCount_AndContinue() {
        // GIVEN Card increment null and velocity is ok
        tx = Transaction.builder().userId(1L).amount(new BigDecimal("10")).build();
        given(valueOperations.increment(startsWith("card_test:"))).willReturn(null);
        given(valueOperations.increment(startsWith("velocity:"))).willReturn(1L);

        // WHEN
        service.check(tx);

        // THEN Didn't throw nullPointer
        verify(valueOperations).increment(startsWith("velocity:"));
    }

    @Test
    @DisplayName("Should not fraud if both Redis returns are null")
    void check_ShouldHandleAllRedisNulls() {
        // GIVEN Every Redis call is null
        when(valueOperations.increment(anyString())).thenReturn(null);

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertFalse(result.isFraud());
    }
}
