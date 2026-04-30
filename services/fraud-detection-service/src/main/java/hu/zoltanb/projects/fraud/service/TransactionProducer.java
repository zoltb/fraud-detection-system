package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionProducer {

    private final KafkaTemplate<String, Transaction> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String,Transaction> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTestTransaction () {
        Transaction testTransaction = new Transaction();
        testTransaction.setTransactionId("Start-999");
        testTransaction.setAmount(new java.math.BigDecimal("15000"));

        log.info("===> SENT: {}", testTransaction.getTransactionId());
        kafkaTemplate.send("transactions", testTransaction);
    }
}
