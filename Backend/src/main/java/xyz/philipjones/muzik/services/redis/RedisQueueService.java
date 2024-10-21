package xyz.philipjones.muzik.services.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisQueueService {

    private static final int MAX_QUEUE_SIZE = 2;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Add to a specific user queue (Track/Album/Artist)
    public void addToQueue(String userId, String queueType, String item) {
        String queueKey = formatQueueKey(userId, queueType);
        Long queueSize = redisTemplate.opsForList().size(queueKey);

        if (queueSize != null && queueSize >= MAX_QUEUE_SIZE) {
            // Log or handle a case when the queue is full
            System.out.println("Queue is full, not adding more items.");
        } else {
            redisTemplate.opsForList().rightPush(queueKey, item);
        }
    }

    // Pop an item from a specific user queue (Track/Album/Artist)
    public String popFromQueue(String userId, String queueType) {
        String queueKey = formatQueueKey(userId, queueType);
        return redisTemplate.opsForList().leftPop(queueKey);
    }

    // Check if a specific user queue is full (queue size == MAX_QUEUE_SIZE)
    public boolean isQueueFull(String userId, String queueType) {
        String queueKey = formatQueueKey(userId, queueType);
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null && size >= MAX_QUEUE_SIZE;
    }

    // Helper to format the Redis queue key
    private String formatQueueKey(String userId, String queueType) {
        return String.format("user:%s:%sQueue", userId, queueType);
    }

    // Optional: Convenience methods to interact with Track, Album, and Artist queues

    public void addToTrackQueue(String userId, String track) {
        addToQueue(userId, "track", track);
    }

    public void addToAlbumQueue(String userId, String album) {
        addToQueue(userId, "album", album);
    }

    public void addToArtistQueue(String userId, String artist) {
        addToQueue(userId, "artist", artist);
    }

    public String popFromTrackQueue(String userId) {
        return popFromQueue(userId, "track");
    }

    public String popFromAlbumQueue(String userId) {
        return popFromQueue(userId, "album");
    }

    public String popFromArtistQueue(String userId) {
        return popFromQueue(userId, "artist");
    }

    public boolean isTrackQueueFull(String userId) {
        return isQueueFull(userId, "track");
    }

    public boolean isAlbumQueueFull(String userId) {
        return isQueueFull(userId, "album");
    }

    public boolean isArtistQueueFull(String userId) {
        return isQueueFull(userId, "artist");
    }
}
