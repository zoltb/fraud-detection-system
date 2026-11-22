package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TransactionProducerTest {

    private static final String TOPIC = "transactions";

    @Mock
    private KafkaTemplate<String, Transaction> kafkaTemplate;

    private TransactionProducer transactionProducer;

    @BeforeEach
    void setup() {
        transactionProducer = new TransactionProducer(kafkaTemplate, TOPIC);
    }

    @Test
    @DisplayName("Check Kafka gets the right topic from TransactionProducer")
    void TestTransaction_shouldSentToCorrectTopic() {
        // GIVEN Add fields
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1234L);
        transaction.setUserId(123L);

        // WHEN
        transactionProducer.sendTransaction(transaction);


        // THEN: kafkaTemplate.send was called with the "transactions" topic
        then(kafkaTemplate).should().send(eq(TOPIC), eq("123"), any(Transaction.class));
    }
}
