package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.model.TransactionEntity;
import hu.zoltanb.projects.fraud.model.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionConsumer {

    private final TransactionRepository repository;
    private final FraudDetectionService fraudService;

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topic}",
            concurrency = "3")
    public void consume(List<ConsumerRecord<String, Transaction>> records, Acknowledgment acknowledgment) {

        List<TransactionEntity> entitiesToSave = new ArrayList<>();

        for (ConsumerRecord<String, Transaction> record : records) {
            Transaction message = record.value();

            FraudCheckResult result = fraudService.check(message);

            TransactionEntity entity = TransactionEntity.builder()
                    .transactionId(message.getTransactionId())
                    .userId(message.getUserId())
                    .amount(message.getAmount())
                    .merchantId(message.getMerchantId())
                    .createdAt(message.getCreatedAt())
                    .fraud(result.isFraud())
                    .fraudTypes(result.fraudTypes())
                    .partition(record.partition())
                    .build();

            entitiesToSave.add(entity);
        }

        repository.saveAll(entitiesToSave);
        log.debug("Saved batch of {} transactions", entitiesToSave.size());

        // Manual ACK: if saveAll() fails, this line is not reached, so Kafka redelivers
        // the batch (at-least-once: no data loss, but duplicates are possible).
        acknowledgment.acknowledge();
    }
}