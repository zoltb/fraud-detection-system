package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
public class TransactionFlowIT {

    @Autowired
    private TransactionProducer producer;
    @Autowired
    private ConsumerFactory<String, Transaction> consumerFactory;

    @Test
    void testTransactionReachesKafkaTopic() throws InterruptedException {
        // Creating test consumer to check a real Kafka
        Consumer<String, Transaction> consumer = consumerFactory.createConsumer("test-group", "test-client");
        consumer.subscribe(Collections.singletonList("transactions"));
        Thread.sleep(5000); // WAIT FOR CONSUMER
        Transaction testTx = Transaction.builder()
                .transactionId(999L)
                .userId(123L)
                .amount(new BigDecimal("150.00"))
                .merchantId(456L)
                .createdAt(LocalDateTime.now())
                .build();

        producer.sendTestTransaction(testTx);
        // Reading data from Kafka
        ConsumerRecords<String, Transaction> records = consumer.poll(Duration.ofSeconds(10));

        // Checking the incoming data
        assertFalse(records.isEmpty(), "NO INCOMING DATA!");

        Transaction received = records.iterator().next().value();
        System.out.println("===> during the test it has transaction ID: " + received.getTransactionId());

        assertNotNull(received.getTransactionId());
        consumer.close();
    }
}