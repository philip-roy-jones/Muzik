package xyz.philipjones.muzik.services.spotify;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.services.random.string.RandomStringErrorService;
import xyz.philipjones.muzik.services.random.string.RandomStringService;
import xyz.philipjones.muzik.services.random.string.UnicodeScriptService;
import xyz.philipjones.muzik.services.redis.RedisQueueService;
import xyz.philipjones.muzik.services.redis.RedisService;

import java.util.HashMap;
import java.util.Map;

@Service
public class SpotifyHarvestService {

    private final RedisQueueService redisQueueService;
    private final RedisService redisService;
    private final SpotifyTokenService spotifyTokenService;
    private final RandomStringService randomStringService;
    private final UnicodeScriptService unicodeScriptService;
    private final SpotifyRequestService spotifyRequestService;
    private final RandomStringErrorService randomStringErrorService;

    public SpotifyHarvestService(RedisQueueService redisQueueService, SpotifyTokenService spotifyTokenService,
                                 RedisService redisService, RandomStringService randomStringService,
                                 UnicodeScriptService unicodeScriptService, SpotifyRequestService spotifyRequestService,
                                 RandomStringErrorService randomStringErrorService) {
        this.redisQueueService = redisQueueService;
        this.spotifyTokenService = spotifyTokenService;
        this.redisService = redisService;
        this.randomStringService = randomStringService;
        this.unicodeScriptService = unicodeScriptService;
        this.spotifyRequestService = spotifyRequestService;
        this.randomStringErrorService = randomStringErrorService;
    }

    @Async
    public void startHarvesting(String username) throws InterruptedException {
        System.out.println("Starting harvesting for user: " + username);
        String harvestingKey = "harvesting:" + username;
        boolean isHarvesting = redisService.getValue(harvestingKey) != null;

        // Mark/remark the user as harvesting
        redisService.setValueWithExpiration(harvestingKey, "true", spotifyTokenService.getSpotifyAccessTokenExpiration(username) + 5000);

        if (isHarvesting) {
            System.out.println("User " + username + " is already harvesting. Renewed redis and returning...");
            return;
        }

        int delay = 3333;

        try {
            // While the spotify access token is not expired, keep harvesting
            while (spotifyTokenService.getSpotifyAccessToken(username) != null) {
                // TODO: This blocks dev tools from being able to stop the thread, have to wait for it to timeout of 30 seconds
                //2024-10-23T08:57:30.771-04:00  INFO 13460 --- [muzik] [       Thread-4] o.s.c.support.DefaultLifecycleProcessor  : Shutdown phase 2147483647 ends with 1 bean still running after timeout of 30000ms: [applicationTaskExecutor]
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Thread interrupted for user: " + username);
                    break;
                }

                // Types of harvesting: tracks, albums, artists
                harvester(username, "track");
                harvester(username, "album");
                harvester(username, "artist");

//                System.out.println("Sleeping for " + delay + " milliseconds");
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Harvesting interrupted for user: " + username);
        }
    }


    // ------------------------------ Private Methods ------------------------------

    private void harvester(String username, String type) {
        while (!redisQueueService.isQueueFull(username, type)) {
            String randomString = randomStringService.generateRandomString(unicodeScriptService.generateRandomScript());
            HashMap response = spotifyRequestService.search(randomString, type, 1, 0, "audio", username);

            Map<String, Object> tracks = (Map<String, Object>) response.get(type + "s");
            int totalResults = (int) tracks.get("total");

            if (totalResults > 0) {
                redisQueueService.addToQueue(username, type, randomString, totalResults);
            } else {
                randomStringErrorService.saveRandomStringError(randomString);
            }
        }
    }
}
