package xyz.philipjones.muzik.services.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Service
public class RedisQueueService {

    private static final int MAX_QUEUE_SIZE = 3;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Add to a specific user queue (Track/Album/Artist)
    public void addToQueue(String username, String queueType, String randomString, int totalResults) {
        String queueKey = formatQueueKey(username, queueType);
        Long queueSize = redisTemplate.opsForList().size(queueKey);

        if (queueSize != null && queueSize >= MAX_QUEUE_SIZE) {
            // Log or handle a case when the queue is full
            System.out.println("Queue is full, not adding more items.");
        } else {
            // HACK: Get the expiration time and set it back to the queue since it is removed when adding an item
            Long expiration = redisTemplate.getExpire(queueKey, TimeUnit.MILLISECONDS);
            redisTemplate.opsForList().rightPush(queueKey, randomString + ":" + totalResults);
            if (expiration != null && expiration > 0) {
                redisTemplate.expire(queueKey, expiration, TimeUnit.MILLISECONDS);
            }
        }
    }

    // Pop an item from a specific user queue (Track/Album/Artist)
    public ArrayList<String> popFromQueue(String username, String queueType) {
        String queueKey = formatQueueKey(username, queueType);
        String item = redisTemplate.opsForList().leftPop(queueKey);

        // Using array to get around the final requirement for lambda
        String[] randomString = {""};
        int[] totalResults = {0};

        // Deconstruct the item to get the random string and total results
        if (item != null) {
            // Split the item by the last colon
            String[] parts = item.split(":(?=[^:]*$)");
            randomString[0] = parts[0];
            totalResults[0] = Integer.parseInt(parts[1]);
        }

        return new ArrayList<String>() {{
            add(randomString[0]);
            add(String.valueOf(totalResults[0]));
        }};
    }

    // Check if a specific user queue is full (queue size == MAX_QUEUE_SIZE)
    public boolean isQueueFull(String username, String queueType) {
        String queueKey = formatQueueKey(username, queueType);
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null && size >= MAX_QUEUE_SIZE;
    }

    // Helper to format the Redis queue key
    public String formatQueueKey(String username, String queueType) {
        return String.format("user:%s:%sQueue", username, queueType);
    }
}
