package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionConsumer {


    @KafkaListener(topics = "transactions", groupId = "fraud-detection-final-v5565")
    public void consume(Transaction transaction) {
        log.info("===> KAFKA MESSAGE ARRIVED: {}", transaction.getTransactionId());

        if (transaction.getAmount().compareTo(new java.math.BigDecimal("30000")) > 0) {
            log.warn("!!!Possible fraud transaction!!! ID:{}, Amount: {} ",
                    transaction.getTransactionId(), transaction.getAmount());
        }
    }
}


