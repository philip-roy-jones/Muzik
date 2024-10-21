package xyz.philipjones.muzik.services.spotify;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.services.redis.RedisQueueService;
import xyz.philipjones.muzik.services.redis.RedisService;

import javax.annotation.PostConstruct;
import java.util.Set;

@Service
public class SpotifyHarvestingService {

    private final RedisQueueService redisQueueService;
    private final RedisService redisService;
    private final SpotifyTokenService spotifyTokenService;

    public SpotifyHarvestingService(RedisQueueService redisQueueService, SpotifyTokenService spotifyTokenService,
                                    RedisService redisService) {
        this.redisQueueService = redisQueueService;
        this.spotifyTokenService = spotifyTokenService;
        this.redisService = redisService;
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
                startHarvesting(username);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Harvesting interrupted for user: " + username);
            }
        }
    }

    @Async
    public void startHarvesting(String username) throws InterruptedException {
        System.out.println("Starting harvesting for user: " + username);
        String harvestingKey = "harvesting:" + username;
        boolean isHarvesting = redisService.getValue(harvestingKey) != null;

        if (isHarvesting) {
            System.out.println("User " + username + " is already harvesting. Skipping...");
            return;
        }

        // Mark the user as harvesting
        redisService.setValue(harvestingKey, "true");
        int delay = 5000;

        try{
            // While the spotify access token is not expired, keep harvesting
            while (spotifyTokenService.getSpotifyAccessToken(username) != null) {

                // Check if the queue is full

                // Fetch data from Spotify and add results and string to the queue if results > 0
                System.out.println("Sleeping for " + delay + " milliseconds");
                Thread.sleep(delay);
            }
        } finally {
            // Remove the user from the harvesting list
            System.out.println("Removing user " + username + " from harvesting list");
            redisService.deleteKey(harvestingKey);
        }
    }
}

