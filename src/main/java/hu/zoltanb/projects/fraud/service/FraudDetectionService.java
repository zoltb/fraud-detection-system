package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.config.FraudAppConfig;
import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final StringRedisTemplate redisTemplate;
    private final FraudAppConfig config;


    public FraudCheckResult check(Transaction tx) {
        var det = config.getDetection();
        //Card testing
        if (tx.getAmount().compareTo(det.getCardTest().getAmountLimit()) < 0) {
            String cKey = "card_test:" + tx.getUserId();
            Long cCount = redisTemplate.opsForValue().increment(cKey);
            if (cCount != null && cCount == 1) redisTemplate.expire(cKey, Duration.ofMinutes(det.getCardTest().getDurationMinutes()));

            if (cCount != null && cCount > det.getCardTest().getCountLimit()) {
                return new FraudCheckResult(true, "CARD_TESTING");
            }
        }
        //Velocity test
        String vKey = "velocity:" + tx.getUserId();
        Long vCount = redisTemplate.opsForValue().increment(vKey);
        if (vCount != null && vCount == 1) redisTemplate.expire(vKey, Duration.ofMinutes(det.getVelocity().getDurationMinutes()));

        if (vCount != null && vCount > det.getVelocity().getCountLimit()) {
            return new FraudCheckResult(true, "VELOCITY");
        }

        return new FraudCheckResult(false, null);
    }


}
