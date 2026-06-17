package hu.zoltanb.projects.fraud.service;
import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.service.detector.FraudDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FraudDetectionServiceTest {

    @Mock
    private FraudDetector cardDetector;

    @Mock
    private FraudDetector velocityDetector;

    private FraudDetectionService service;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        // Injects mocks into the service
        service = new FraudDetectionService(List.of(cardDetector, velocityDetector));

        tx = Transaction.builder()
                .userId(1L)
                .amount(new BigDecimal("100"))
                .build();
    }

    @Test
    @DisplayName("Run without fraud")
    void check_ShouldReturnNoFraud() {
        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertFalse(result.isFraud());
        assertTrue(result.fraudTypes().isEmpty());
        // Ellenőrizzük, hogy mindkét detektor meghívódott
        verify(cardDetector).check(eq(tx), any());
        verify(velocityDetector).check(eq(tx), any());
    }

    @Test
    @DisplayName("If detector catches a fraud then the service shows it")
    void check_ShouldReturnFraud_WhenDetectorFindsOne() {
        // GIVEN: A cardDetector hozzáad egy típust
        doAnswer(invocation -> {
            List<String> fraudTypes = invocation.getArgument(1);
            fraudTypes.add("CARD_TESTING");
            return null;
        }).when(cardDetector).check(any(), any());

        // WHEN
        FraudCheckResult result = service.check(tx);

        // THEN
        assertTrue(result.isFraud());
        assertTrue(result.fraudTypes().contains("CARD_TESTING"));
    }
}
