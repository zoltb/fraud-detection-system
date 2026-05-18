package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.model.TransactionEntity;
import hu.zoltanb.projects.fraud.model.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionConsumerTest {

    @Mock
    private ConsumerRecordMetadata metadata;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private TransactionRepository repository;

    @Mock
    private OpenSearchClient osClient;

    @Mock
    private KafkaTemplate<String, Transaction> kafkaTemplate;

    @InjectMocks
    private TransactionConsumer transactionConsumer;

    @Test
    @DisplayName("Check transaction process and the save transaction")
    void consume_ShouldProcessAndSaveTransaction() {
        // GIVEN
        Transaction message = new Transaction();
        message.setTransactionId(999L);
        message.setAmount(BigDecimal.valueOf(34.5));
        message.setMerchantId(23L);
        message.setCreatedAt(LocalDateTime.now());

        FraudCheckResult mockResult = new FraudCheckResult(false, null);
        given(fraudDetectionService.check(message)).willReturn(mockResult);

        // WHEN
        transactionConsumer.consume(message,metadata);

        // THEN Assert check of the fraudDetectionService
        then(fraudDetectionService).should().check(message);
        then(repository).should().save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Check the save in case of fraud detected")
    void consume_WhenFraudDetected_ShouldSave() {
        // GIVEN
        Transaction message = new Transaction();
        FraudCheckResult fraudCheckResult = new FraudCheckResult(true, "VELOCITY");

        given(fraudDetectionService.check(message)).willReturn(fraudCheckResult);
        // WHEN
        transactionConsumer.consume(message, metadata);
        // THEN
        then(repository).should().save(argThat(TransactionEntity::isFraud));
    }
}
