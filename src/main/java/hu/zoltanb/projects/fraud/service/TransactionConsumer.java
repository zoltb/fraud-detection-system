package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.model.TransactionEntity;
import hu.zoltanb.projects.fraud.model.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
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
    private final OpenSearchClient osClient;
    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    // Using getUserId to have data in the same partition
    @KafkaListener(
            topics = "${app.kafka.topic}",
            //Auto-cleanup for Kafka
            groupId = "${app.kafka.consumer-group}-#{T(java.util.UUID).randomUUID().toString().substring(0,5)}",
            concurrency = "3")
    public void consume(Transaction message,
                        ConsumerRecordMetadata metadata) {
        int partitionId = metadata.partition();
        log.info("===> THREAD: {} | PARTITION: {} | TransactionId: {}",
                Thread.currentThread().getName(),
                partitionId,
                message.getTransactionId());
        //Redis
        FraudCheckResult result = fraudService.check(message);

        //Create entity
        TransactionEntity entity = TransactionEntity.builder()
                .transactionId(message.getTransactionId()).userId(message.getUserId())
                .amount(message.getAmount()).merchantId(message.getMerchantId())
                .createdAt(message.getCreatedAt()).fraud(result.isFraud())
                .fraudType(result.fraudType()).partition(partitionId).build();

        // save into PostgreSQL
        repository.save(entity);
        log.info("Transaction saved. Fraud status: {}", result.isFraud());

        //notify if it is fraud
        if (result.isFraud()) {
            log.warn("!!!Possible fraud transaction!!! ID:{}, Amount: {} ",
                    message.getTransactionId(), message.getAmount());
        }

        try {
            java.util.Map<String, Object> osDocument = new java.util.HashMap<>();
            osDocument.put("transactionId", message.getTransactionId());
            osDocument.put("userId", message.getUserId());
            osDocument.put("amount", message.getAmount() != null ? message.getAmount().doubleValue() : 0.0);
            osDocument.put("merchantId", message.getMerchantId());
            osDocument.put("createdAt", message.getCreatedAt() != null ? message.getCreatedAt().toString() : "");
            osDocument.put("isFraud", result.isFraud());
            osDocument.put("fraudType", result.fraudType() == null ? "CLEAN" : result.fraudType());
            osDocument.put("partitionId", partitionId);

            // IndexRequest összeállítása a modern builder stílusban
            org.opensearch.client.opensearch.core.IndexRequest<java.util.Map<String, Object>> request =
                    org.opensearch.client.opensearch.core.IndexRequest.of(i -> i
                            .index("transactions")
                            .id(message.getTransactionId() != null ? message.getTransactionId().toString() : java.util.UUID.randomUUID().toString())
                            .document(osDocument)
                    );

            osClient.index(request);
            log.info("Transaction {} successfully indexed in OpenSearch.", message.getTransactionId());

        } catch (Exception e) {
            log.error("Failed to index transaction in OpenSearch", e);
        }
        // ==============================================================
    }
}


