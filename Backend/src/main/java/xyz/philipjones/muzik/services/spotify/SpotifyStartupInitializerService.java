package xyz.philipjones.muzik.services.spotify;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.services.random.string.RandomStringService;
import xyz.philipjones.muzik.services.random.string.UnicodeScriptService;
import xyz.philipjones.muzik.services.redis.RedisQueueService;
import xyz.philipjones.muzik.services.redis.RedisService;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SpotifyHarvestingService {

    private final RedisService redisService;
    private final SpotifyAsyncService spotifyAsyncService;

    public SpotifyHarvestingService(RedisService redisService, SpotifyAsyncService spotifyAsyncService) {
        this.redisService = redisService;
        this.spotifyAsyncService = spotifyAsyncService;
    }

    // Start harvesting for all users that were harvesting before the server was restarted
    @PostConstruct
    public void initializeHarvesting() {
        Set<String> keys = redisService.getKeys("harvesting:*");
        for (String key : keys) {
            String username = key.split(":")[1];
            try {
                // Deleting key so startHarvesting() will see there is no key and start
                redisService.deleteKey(key);
                System.out.println("Before startHarvesting");
                spotifyAsyncService.startHarvesting(username);
                System.out.println("Got past startHarvesting");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Harvesting interrupted for user: " + username);
            }
        }
    }
}

