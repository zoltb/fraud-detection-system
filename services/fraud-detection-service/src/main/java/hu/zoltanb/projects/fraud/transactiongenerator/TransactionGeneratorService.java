package hu.zoltanb.projects.fraud.transactiongenerator;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
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
    private final FraudAppConfig config;
    //Transaction ID need to be incremental and unique
    private final AtomicLong txIdCounter = new AtomicLong(1);

    public TransactionGeneratorService(TransactionProducer producer, FraudAppConfig config) {
        this.producer = producer;
        this.config = config;
    }

    public void generateData(int count) throws InterruptedException {
        var gen = config.getGenerator();
        //max. number of user is 20
        long minUserId = gen.getMinUserId();
        long maxUserId = gen.getMaxUserId();

        long minMerchantId = gen.getMinMerchantId();
        long maxMerchantId = gen.getMaxMerchantId();

        for (int i = 0; i < count; i++) {
            //Because ThreadLocalrandom exlusive upper bound
            long randomUserId = ThreadLocalRandom.current().nextLong(minUserId, maxUserId + 1);
            long randomMerchantId = ThreadLocalRandom.current().nextLong(minMerchantId, maxMerchantId + 1);

            Transaction tx = Transaction.builder()
                    .transactionId(txIdCounter.getAndIncrement())   //UNIQUE!
                    .userId(randomUserId)                           //REPEATING
                    .amount(new BigDecimal(ThreadLocalRandom.current()
                            .nextDouble(gen.getMinAmount(), gen.getMaxAmount()))
                            .setScale(2, RoundingMode.HALF_UP))
                    .merchantId(randomMerchantId)
                    .createdAt(LocalDateTime.now())
                    .build();

            producer.sendTestTransaction(tx);
            Thread.sleep(gen.getSleepMs());
        }
    }
}
