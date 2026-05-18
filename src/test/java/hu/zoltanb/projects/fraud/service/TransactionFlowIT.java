package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = { "listeners=PLAINTEXT://localhost:39092", "port=39092" }
)
public class TransactionFlowIT {

    @Autowired
    private TransactionProducer producer;
    @Autowired
    private ConsumerFactory<String, Transaction> consumerFactory;

    @Value("${app.kafka.consumer-group:fraud-detection-test-group}")
    private String baseConsumerGroup;

    @Test
    void testTransactionReachesKafkaTopic() throws InterruptedException {
        String uniqueSuffix = "-" + UUID.randomUUID().toString().substring(0, 5);
        String finalGroupId = baseConsumerGroup + uniqueSuffix;
        String finalClientId = "test-client" + uniqueSuffix;
        // Unique group name - earlier run won't cause problem

        Consumer<String, Transaction> consumer = consumerFactory.createConsumer(finalGroupId, finalClientId);
        consumer.subscribe(Collections.singletonList("transactions"));

        // Consumert with empty poll, a Kafka will add partitions
        consumer.poll(Duration.ofMillis(100));

        Transaction testTx = Transaction.builder()
                .transactionId(999L)
                .userId(123L)
                .amount(new BigDecimal("150.00"))
                .merchantId(456L)
                .createdAt(LocalDateTime.now())
                .build();

        producer.sendTestTransaction(testTx);
        // Reading data from Kafka
        final ConsumerRecords<String, Transaction>[] recordsHolder = new ConsumerRecords[1];

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    ConsumerRecords<String, Transaction> polledRecords = consumer.poll(Duration.ofMillis(50));
                    assertFalse(polledRecords.isEmpty(), "If topic empty...");
                    recordsHolder[0] = polledRecords; // Saving record for check
                });
        // Checking the incoming data
        Transaction received = recordsHolder[0].iterator().next().value();
        System.out.println("===> during the test it has transaction ID: " + received.getTransactionId());


        assertNotNull(received.getTransactionId());
        consumer.close();
    }
}