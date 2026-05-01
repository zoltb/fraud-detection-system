package hu.zoltanb.projects.fraud.transactiongenerator;

import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.service.TransactionProducer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransactionGeneratorService {

    private final TransactionProducer producer;
    //Transaction ID need to be incremental and unique
    private final AtomicLong txIdCounter = new AtomicLong(1);

    public TransactionGeneratorService(TransactionProducer producer) {
        this.producer = producer;
    }

    public void generateData(int count) throws InterruptedException {
        //max. number of user is 10
        long minUserId = 100;
        long maxUserId = 110;

        for (int i = 0; i < count; i++) {
            //Because ThreadLocalrandom exlusive upper bound
            long randomUserId = ThreadLocalRandom.current().nextLong(minUserId, maxUserId + 1);

            Transaction tx = Transaction.builder()
                    .transactionId(txIdCounter.getAndIncrement())   //UNIQUE!
                    .userId(randomUserId)                           //REPEATING
                    .amount(new BigDecimal(ThreadLocalRandom.current()
                            .nextDouble(50, 10001))
                            .setScale(2, RoundingMode.HALF_UP))
                    .currency("HUF")
                    .createdAt(LocalDateTime.now())
                    .build();

            producer.sendTestTransaction(tx);
            Thread.sleep(10);
        }
    }
}
