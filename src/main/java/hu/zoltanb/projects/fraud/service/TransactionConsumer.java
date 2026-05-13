package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.model.TransactionEntity;
import hu.zoltanb.projects.fraud.model.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionConsumer {

    private final TransactionRepository repository;
    private final FraudDetectionService fraudService;
    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    // Using getUserId to have data in the same partition
    @KafkaListener(
            topics = "transactions",
            //Auto-cleanup for Kafka
            groupId = "fraud-group-#{T(java.util.UUID).randomUUID().toString()}",
            concurrency = "3")
    public void consume(Transaction message,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partitionId) {
        log.info("===> KAFKA MESSAGE ARRIVED on PARTITION: {} | TransactionId: {} | User: {}",
                partitionId, message.getTransactionId(), message.getUserId());

        //Redis
        FraudCheckResult result = fraudService.check(message);

        //Create entity
        TransactionEntity entity = TransactionEntity.builder()
                .transactionId(message.getTransactionId())
                .userId(message.getUserId())
                .amount(message.getAmount())
                .merchantId(message.getMerchantId())
                .createdAt(message.getCreatedAt())
                .fraud(result.isFraud())
                .fraudType(result.fraudType())
                .partition(partitionId)
                .build();

        // save into PostgreSQL
        repository.save(entity);
        log.info("Transaction saved. Fraud status: {}", result.isFraud());

        //notify if it is fraud
        if (result.isFraud()) {
            log.warn("!!!Possible fraud transaction!!! ID:{}, Amount: {} ",
                    message.getTransactionId(), message.getAmount());
        }
    }

}


