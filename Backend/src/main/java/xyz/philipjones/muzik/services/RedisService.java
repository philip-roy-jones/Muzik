package xyz.philipjones.muzik.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void setValueWithExpiration(String key, String value, long timeoutInMillis) {
        if (timeoutInMillis <= 0) {
            throw new IllegalArgumentException("Expiration time must be greater than zero");
        }
        redisTemplate.opsForValue().set(key, value, timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }
}