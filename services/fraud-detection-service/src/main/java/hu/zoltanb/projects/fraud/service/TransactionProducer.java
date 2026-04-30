package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionProducer {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, Transaction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTestTransaction(Transaction transaction) {
        log.info("===> SENT: {}", transaction.getTransactionId());
        kafkaTemplate.send("transactions", transaction);
    }
}
