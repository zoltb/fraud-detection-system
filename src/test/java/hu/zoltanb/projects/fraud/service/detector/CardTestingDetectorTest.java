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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardTestingDetectorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FraudAppConfig config;

    @InjectMocks
    private CardTestingDetector detector;

    private FraudAppConfig.Detection detection;
    private FraudAppConfig.Detection.CardTest cardTestConfig;

    @BeforeEach
    void setUp() {
        detection = new FraudAppConfig.Detection();
        cardTestConfig = new FraudAppConfig.Detection.CardTest();
        cardTestConfig.setAmountLimit(new BigDecimal("200"));
        cardTestConfig.setCountLimit(2);
        cardTestConfig.setDurationMinutes(10);
        detection.setCardTest(cardTestConfig);

        lenient().when(config.getDetection()).thenReturn(detection);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Not adding to the list if the amount is under the limit")
    void shouldNotFlagWhenAmountIsHigh() {
        Transaction tx = Transaction.builder().amount(new BigDecimal("300")).build();
        List<String> fraudTypes = new ArrayList<>();

        detector.check(tx, fraudTypes);

        assertFalse(fraudTypes.contains("CARD_TESTING"));
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("Set expiry when the counter is 1")
    void shouldSetExpiryWhenCountIsOne() {
        Transaction tx = Transaction.builder().userId(1L).amount(new BigDecimal("100")).build();
        when(valueOperations.increment(anyString())).thenReturn(1L);
        List<String> fraudTypes = new ArrayList<>();

        detector.check(tx, fraudTypes);

        verify(redisTemplate).expire(eq("card_test:1"), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("Adds CARD_TESTING type if it exceeds the limit")
    void shouldFlagWhenLimitExceeded() {
        Transaction tx = Transaction.builder().userId(1L).amount(new BigDecimal("100")).build();
        // The limit 2, so the 3 is fraud
        when(valueOperations.increment(anyString())).thenReturn(3L);
        List<String> fraudTypes = new ArrayList<>();

        detector.check(tx, fraudTypes);

        assertTrue(fraudTypes.contains("CARD_TESTING"));
    }
}