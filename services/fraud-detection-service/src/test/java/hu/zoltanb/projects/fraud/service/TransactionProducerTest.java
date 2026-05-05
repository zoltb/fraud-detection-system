package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, Transaction> kafkaTemplate;

    @InjectMocks
    private TransactionProducer transactionProducer;

    @Test
    @DisplayName("Check kafka gets the right topic from TransactionProducer")
    void SenTestTransaction_shouldSentToCorrectTopic() {
        //Add fields
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1234L);
        transactionProducer.sendTestTransaction(transaction);
        // Assert
        // Verification: kafkaTemplate.send was called with the "transactions" topic
        verify(kafkaTemplate).send(eq("transactions"), eq(transaction));
    }

}
