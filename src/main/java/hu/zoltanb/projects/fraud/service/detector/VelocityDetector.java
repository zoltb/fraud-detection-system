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
public class VelocityDetector implements FraudDetector {
    private final StringRedisTemplate redisTemplate;
    private final FraudAppConfig config;

    @Override
    public void check(Transaction tx, List<String> fraudTypes) {
        var velocityCfg = config.getDetection().getVelocity();
        String vKey = "velocity:" + tx.getUserId();
        Long vCount = redisTemplate.opsForValue().increment(vKey);
        if (vCount != null && vCount == 1) {
            redisTemplate.expire(vKey, Duration.ofMinutes(velocityCfg.getDurationMinutes()));
        }
        if (vCount != null && vCount > velocityCfg.getCountLimit()) {
            fraudTypes.add("VELOCITY");
        }
    }
}