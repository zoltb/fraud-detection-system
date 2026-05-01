package hu.zoltanb.projects.fraud.service;

import io.lettuce.core.dynamic.annotation.CommandNaming;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisCleaner {

    private final StringRedisTemplate redisTemplate;

    public RedisCleaner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    //Will run in case of every new initialization
    @PostConstruct
    public void cleanRedisOnStart() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.info("Redis cache is empty!");
    }
}
