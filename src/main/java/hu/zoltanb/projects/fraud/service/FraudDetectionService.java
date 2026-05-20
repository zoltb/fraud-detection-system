package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final StringRedisTemplate redisTemplate;
    private final FraudAppConfig config;


    public FraudCheckResult check(Transaction tx) {

        if (tx == null || tx.getAmount() == null || tx.getUserId() == null) {
            return new FraudCheckResult(false, new ArrayList<>());
        }

        var det = config.getDetection();



        // Card testing
        CompletableFuture<String> cardTestFuture = CompletableFuture.supplyAsync(() -> {
            log.info("[{}] Card testing is running on: {}", tx.getTransactionId(), Thread.currentThread().getName());
            if (tx.getAmount().compareTo(det.getCardTest().getAmountLimit()) < 0) {
                String cKey = "card_test:" + tx.getUserId();
                Long cCount = redisTemplate.opsForValue().increment(cKey);
                if (cCount != null && cCount == 1) {
                    redisTemplate.expire(cKey, Duration.ofMinutes(det.getCardTest().getDurationMinutes()));
                }

                if (cCount != null && cCount > det.getCardTest().getCountLimit()) {
                    return "CARD_TESTING";
                }
            }
            return null; // If it is not a fraud it is returning null
        });

        // Velocity test runs everytime
        CompletableFuture<String> velocityFuture = CompletableFuture.supplyAsync(() -> {
            log.info("[Tx: {}] Velocity ellenőrzése fut a szálon: {}",
                    tx.getTransactionId(), Thread.currentThread().getName());

            String vKey = "velocity:" + tx.getUserId();
            Long vCount = redisTemplate.opsForValue().increment(vKey);
            if (vCount != null && vCount == 1) {
                redisTemplate.expire(vKey, Duration.ofMinutes(det.getVelocity().getDurationMinutes()));
            }

            if (vCount != null && vCount > det.getVelocity().getCountLimit()) {
                return "VELOCITY";
            }
            return null;
        });
        // Waiting for threads
        CompletableFuture.allOf(cardTestFuture, velocityFuture).join();

        List<String> fraudTypes = new ArrayList<>();

        String cardResult = cardTestFuture.join();
        String velocityResult = velocityFuture.join();

        if (cardResult != null) fraudTypes.add(cardResult);
        if (velocityResult != null) fraudTypes.add(velocityResult);

        boolean isFraud = !fraudTypes.isEmpty();

        return new FraudCheckResult(isFraud, fraudTypes);

    }
}
