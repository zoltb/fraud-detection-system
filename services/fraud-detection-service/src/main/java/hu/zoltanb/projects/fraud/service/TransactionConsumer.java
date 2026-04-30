package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionConsumer {


    /*@KafkaListener(topics = "transactions", groupId = "utolso-utan-is-van-remeny-v1")
    public void consume(String message) {
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.err.println("MEGÉRKEZETT: " + message);
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
}

    @KafkaListener(topics = "transactions", groupId = "fraud-group-test")
    public void listenRaw(String rawMessage) {
        System.out.println("DEBUG: Nyers üzenet érkezett a Kafkából: " + rawMessage);
    }*/

    @KafkaListener(topics = "transactions", groupId = "fraud-group-clean-start")
    public void consume(Transaction transaction){
        log.info("===> KAFKA MESSAGE ARRIVED: {}", transaction.getTransactionId());

        if(transaction.getAmount().compareTo(new java.math.BigDecimal("10000"))>0){
            log.warn("!!!Possible fraud transaction!!! ID:{}, Amount: {} ",
            transaction.getTransactionId(),transaction.getAmount());
        }
    }
}

