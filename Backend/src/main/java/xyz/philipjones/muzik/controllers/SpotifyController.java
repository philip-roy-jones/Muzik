package xyz.philipjones.muzik.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.services.redis.RedisQueueService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.spotify.SpotifyCollectionService;
import xyz.philipjones.muzik.services.spotify.SpotifyHarvestService;
import xyz.philipjones.muzik.services.spotify.SpotifyRequestService;
import xyz.philipjones.muzik.services.spotify.SpotifyTokenService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/spotify")
public class SpotifyController {

    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyRequestService spotifyRequestService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final SpotifyCollectionService spotifyCollectionService;
    private final RedisQueueService redisQueueService;
    private final SpotifyHarvestService spotifyHarvestService;

    @Autowired
    public SpotifyController(SpotifyTokenService spotifyTokenService,
                             SpotifyRequestService spotifyRequestService,
                             ServerAccessTokenService serverAccessTokenService,
                             SpotifyCollectionService spotifyCollectionService,
                             RedisQueueService redisQueueService, SpotifyHarvestService spotifyHarvestService) {
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyRequestService = spotifyRequestService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.spotifyCollectionService = spotifyCollectionService;
        this.redisQueueService = redisQueueService;
        this.spotifyHarvestService = spotifyHarvestService;
    }

    // ----------------------------------------Auth Routes----------------------------------------
    @GetMapping("/authorize")
    public HashMap<String, String> getCode(@RequestHeader("Authorization") String authorizationHeader) throws NoSuchAlgorithmException {
        String authorizationUrl = spotifyTokenService.getAuthorizationUrl(authorizationHeader.substring("Bearer ".length()));
        HashMap<String, String> result = new HashMap<>();
        result.put("authorizationUrl", authorizationUrl);
        return result;
    }

    @GetMapping("/callback")
    public HashMap<String, String> getToken(@RequestParam("code") String code, @RequestParam("state") String receivedState) throws IOException {
        try {
            return spotifyTokenService.handleSpotifyCallback(code, receivedState);
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    @DeleteMapping("/remove-connection")
    public void removeConnection(@RequestHeader("Authorization") String authorizationHeader) {
        spotifyTokenService.removeConnection(authorizationHeader.substring("Bearer ".length()));
    }


    // ----------------------------------------Spotify API Routes----------------------------------------
    @GetMapping("/random-track")
    public HashMap getRandomTrack(@RequestHeader("Authorization") String authorizationHeader) {
        String username = serverAccessTokenService.getClaimsFromToken(authorizationHeader.substring("Bearer ".length())).getSubject();
        int limit = 1;
        int offset = 0;

        // Pop from track queue to get random string and total results
        //  and immediately async harvest a new track
        ArrayList<String> poppedItem = redisQueueService.popFromQueue(serverAccessTokenService.getClaimsFromToken(authorizationHeader.substring("Bearer ".length())).getSubject(), "track");
        spotifyHarvestService.harvestOne(username, "track");

        // Generate a random offset based on the total results
        Random random = new Random();
        offset = random.nextInt(Integer.parseInt(poppedItem.get(1)));

        HashMap spotifyResponse = spotifyRequestService.search(poppedItem.get(0), "track", limit, offset, "audio", username);

        HashMap tracks = (HashMap) spotifyResponse.get("tracks");
        ArrayList items = (ArrayList) tracks.get("items");

        // Theoretically, there shouldn't be 0 items as the queue should only be populated with the strings with results
        //  Unless in between the time the queue was populated and the time the user requests a random track, Spotify removes tracks
        //  and the user happened to get a random offset greater than the total tracks
        if (items.isEmpty()) {
            return null;
        }

        HashMap randomTrack = (HashMap) items.getFirst();

        spotifyCollectionService.createAndSaveTrackWithAlbumAndArtists(randomTrack);

        return randomTrack;
    }

    @GetMapping("/test")
    public String test(@RequestHeader("Authorization") String authorizationHeader) {

        return "hello world";
    }
}