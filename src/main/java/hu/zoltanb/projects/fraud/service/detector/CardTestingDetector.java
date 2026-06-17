package hu.zoltanb.projects.fraud.service.detector;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CardTestingDetector implements FraudDetector {
    private final StringRedisTemplate redisTemplate;
    private final FraudAppConfig config;

    @Override
    public void check(Transaction tx, List<String> fraudTypes) {
        var cardTestCfg = config.getDetection().getCardTest();
        if (tx.getAmount().compareTo(cardTestCfg.getAmountLimit()) < 0) {
            String cKey = "card_test:" + tx.getUserId();
            Long cCount = redisTemplate.opsForValue().increment(cKey);
            if (cCount != null && cCount == 1) {
                redisTemplate.expire(cKey, Duration.ofMinutes(cardTestCfg.getDurationMinutes()));
            }
            if (cCount != null && cCount > cardTestCfg.getCountLimit()) {
                fraudTypes.add("CARD_TESTING");
            }
        }
    }
}