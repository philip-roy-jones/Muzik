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
    public void initSpotifyQueues(String username) {
        System.out.println("Initializing queues for user: " + username);

        // Set/reset expiration for each queue
        redisService.setExpiration(redisQueueService.formatQueueKey(username, "track"), 1000*60*60);
        redisService.setExpiration(redisQueueService.formatQueueKey(username, "album"), 1000*60*60);
        redisService.setExpiration(redisQueueService.formatQueueKey(username, "artist"), 1000*60*60);

        // Initializing each queue breadth first until full
        while (!redisQueueService.isQueueFull(username, "track") || !redisQueueService.isQueueFull(username, "album") || !redisQueueService.isQueueFull(username, "artist")) {
            if (!redisQueueService.isQueueFull(username, "track")) {
                harvest(username, "track");
            }
            if (!redisQueueService.isQueueFull(username, "album")) {
                harvest(username, "album");
            }
            if (!redisQueueService.isQueueFull(username, "artist")) {
                harvest(username, "artist");
            }
        }
    }

    @Async
    public void harvestOne(String username, String type) {
        harvest(username, type);
    }

    // ------------------------------ Private Methods ------------------------------

    private void harvest(String username, String type) {
            int totalResults = 0;
            String randomString = "";
            HashMap response;
            Map<String, Object> tracks;

            while (totalResults == 0) {
                randomString = randomStringService.generateRandomString(unicodeScriptService.generateRandomScript());
                response = spotifyRequestService.search(randomString, type, 1, 0, "audio", username);
                tracks = (Map<String, Object>) response.get(type + "s");
                totalResults = (int) tracks.get("total");

                if (totalResults == 0) {
//                    System.out.println("No results found for " + type + " with random string: " + randomString);
                    randomStringErrorService.saveRandomStringError(randomString);
                }
            }

            redisQueueService.addToQueue(username, type, randomString, totalResults);
    }
}
