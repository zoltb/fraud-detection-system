package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.model.TransactionEntity;
import hu.zoltanb.projects.fraud.model.TransactionRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TransactionConsumerTest {

    @Mock
    private ConsumerRecordMetadata metadata;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private TransactionRepository repository;

    @Mock
    private Acknowledgment acknowledgment;

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

        ConsumerRecord<String, Transaction> record = new ConsumerRecord<>("topic-nev", 0, 0L, "key", message);
        List<ConsumerRecord<String, Transaction>> records = Collections.singletonList(record);
        FraudCheckResult mockResult = new FraudCheckResult(false, null);
        given(fraudDetectionService.check(message)).willReturn(mockResult);

        // WHEN
        transactionConsumer.consume(records,acknowledgment);

        // THEN Assert check of the fraudDetectionService
        then(fraudDetectionService).should().check(message);
        then(repository).should().saveAll(anyIterable());
    }

    @Test
    @DisplayName("Check the save in case of fraud detected")
    void consume_WhenFraudDetected_ShouldSave() {
        // GIVEN
        Transaction message = new Transaction();
        message.setTransactionId(999L);
        message.setAmount(BigDecimal.valueOf(34.5));
        message.setMerchantId(23L);
        message.setCreatedAt(LocalDateTime.now());

        ConsumerRecord<String, Transaction> record = new ConsumerRecord<>("topic-nev", 0, 0L, "key", message);
        List<ConsumerRecord<String, Transaction>> records = Collections.singletonList(record);

        FraudCheckResult fraudCheckResult = new FraudCheckResult(true, List.of("VELOCITY"));

        given(fraudDetectionService.check(message)).willReturn(fraudCheckResult);
        // WHEN
        transactionConsumer.consume(records, acknowledgment);
        // THEN
        then(fraudDetectionService).should().check(message);
        then(repository).should().saveAll(argThat(list -> ((List<TransactionEntity>)list).get(0).isFraud()));
        then(acknowledgment).should().acknowledge();
    }
}
