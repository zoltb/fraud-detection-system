package hu.zoltanb.projects.fraud.transactiongenerator;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.Transaction;
import hu.zoltanb.projects.fraud.service.TransactionProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class TransactionGeneratorService {

    private final TransactionProducer producer;
    private final RestClient restClient;
    private final FraudAppConfig config;
    //Transaction ID need to be incremental and unique
    private final AtomicLong txIdCounter = new AtomicLong(1);

    public void generateData(int count) throws InterruptedException {
        var gen = config.getGenerator();

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
    }
}
