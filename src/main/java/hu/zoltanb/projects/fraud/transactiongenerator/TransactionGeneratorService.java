package hu.zoltanb.projects.fraud.transactiongenerator;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.service.TransactionProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionGeneratorService {

    private final TransactionProducer producer;
    private final RestClient restClient;
    private final FraudAppConfig config;
    //Transaction ID need to be incremental and unique
    private final AtomicLong txIdCounter = new AtomicLong(1);

    public TransactionGeneratorService(TransactionProducer producer,
                                       RestClient.Builder restClientBuilder,
                                       FraudAppConfig config) {
        this.producer = producer;
        this.config = config;
        this.restClient = restClientBuilder.baseUrl("http://localhost:8080").build();
    }

    public void generateData(int count) throws InterruptedException {
        var gen = config.getGenerator();

        long startTime = System.currentTimeMillis();
        log.info("Starting generation and sending of {} message...", count);

        for (int i = 0; i < count; i++) {
            try {
                // GET API
                Transaction apiTx = restClient.get()
                        .uri("http://localhost:8080/api/mock/transactions")
                        .retrieve()
                        .body(Transaction.class);

                if (apiTx != null) {
                    //Add transaction ID and date
                    Transaction finalTx = apiTx.toBuilder()
                            .transactionId(txIdCounter.getAndIncrement())
                            .createdAt(LocalDateTime.now())
                            .build();

                    // sending to Kafka
                    producer.sendTestTransaction(finalTx);
                }
            } catch (Exception e) {
                System.err.println("Error during API call: " + e.getMessage());
            }
            Thread.sleep(gen.getSleepMs());
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("PRODUCER is ready, the: " + count + " messages were sent in " + duration + " ms.");
    }
}
