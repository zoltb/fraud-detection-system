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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
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
            //Auto-cleanup for Kafka
            groupId = "${app.kafka.consumer-group}-#{T(java.util.UUID).randomUUID().toString().substring(0,5)}",
            concurrency = "3")
    public void consume(List<ConsumerRecord<String, Transaction>> records, Acknowledgment acknowledgment) {

        List<TransactionEntity> entitiesToSave = new ArrayList<>();

        log.info("===> BATCH RECEIVED: {} records on THREAD: {}", records.size(), Thread.currentThread().getName());
        for (ConsumerRecord<String, Transaction> record : records) {
            Transaction message = record.value();
            int partitionId = record.partition();

            // Redis ellenőrzés
            FraudCheckResult result = fraudService.check(message);

            // Entity létrehozása
            TransactionEntity entity = TransactionEntity.builder()
                    .transactionId(message.getTransactionId())
                    .userId(message.getUserId())
                    .amount(message.getAmount())
                    .merchantId(message.getMerchantId())
                    .createdAt(message.getCreatedAt())
                    .fraud(result.isFraud())
                    .fraudTypes(result.fraudTypes())
                    .partition(partitionId)
                    .build();

            entitiesToSave.add(entity);
        }

        // Batch mentés a PostgreSQL-be
        repository.saveAll(entitiesToSave);
        log.info("Batch of {} transactions successfully saved.", entitiesToSave.size());

        // Kézi nyugtázás (ACK): Ha a saveAll elszáll, ez nem fut le,
        // így a Kafka újra ki fogja osztani az üzeneteket -> NINCS ADATVESZTÉS
        acknowledgment.acknowledge();
    }
}


