package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionProducer {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;
    private final String topic;

    public TransactionProducer(KafkaTemplate<String, Transaction> kafkaTemplate,
                               @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendTransaction(Transaction transaction) {
        log.debug("===> SENT: {}", transaction.getTransactionId());
        kafkaTemplate.send(topic, transaction.getUserId().toString(), transaction);
    }
}
