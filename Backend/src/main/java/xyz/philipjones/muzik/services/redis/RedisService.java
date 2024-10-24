package xyz.philipjones.muzik.services.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setValueWithExpiration(String key, String value, long timeoutInMillis) {
        if (timeoutInMillis <= 0) {
            throw new IllegalArgumentException("Expiration time must be greater than zero");
        }
        redisTemplate.opsForValue().set(key, value, timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Set<String> getKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public long getExpiration(String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public void setExpiration(String key, long timeoutInMillis) {
        if (timeoutInMillis <= 0) {
            throw new IllegalArgumentException("Expiration time must be greater than zero");
        }
        redisTemplate.expire(key, timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }
}