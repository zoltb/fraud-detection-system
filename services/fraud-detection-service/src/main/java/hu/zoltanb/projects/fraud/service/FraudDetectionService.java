package hu.zoltanb.projects.fraud.service;

import hu.zoltanb.projects.fraud.model.FraudCheckResult;
import hu.zoltanb.projects.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final StringRedisTemplate redisTemplate;

    public FraudCheckResult check(Transaction tx){
        //Velocity test
        String vKey = "velocity: " + tx.getUserId();
        Long vCount = redisTemplate.opsForValue().increment(vKey);
        if (vCount != null && vCount == 1) redisTemplate.expire(vKey, Duration.ofMinutes(1));

        if (vCount != null && vCount > 5) {
            return new FraudCheckResult(true,"VELOCITY");
        }
        //Card testing
        if (tx.getAmount().compareTo(new BigDecimal(200)) < 0) {
            String cKey = "card_test: " + tx.getUserId();
            Long cCount = redisTemplate.opsForValue().increment(cKey);
            if (cCount != null && cCount == 1) redisTemplate.expire(cKey, Duration.ofMinutes(10));

            if (cCount != null && cCount > 3) {
                return new FraudCheckResult(true, "CARD_TESTING");
            }
        }

        return new FraudCheckResult(false, null);

    }



}
