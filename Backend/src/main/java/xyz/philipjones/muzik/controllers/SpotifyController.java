package xyz.philipjones.muzik.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.services.redis.RedisQueueService;
import xyz.philipjones.muzik.services.redis.RedisService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.spotify.SpotifyCollectionService;
import xyz.philipjones.muzik.services.spotify.SpotifyHarvestService;
import xyz.philipjones.muzik.services.spotify.SpotifyRequestService;
import xyz.philipjones.muzik.services.spotify.SpotifyTokenService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/spotify")
@CrossOrigin(origins = "${frontend.https.url}", allowCredentials = "true")
public class SpotifyController {

    @Value("${frontend.https.url}")
    private String frontendUrl;

    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyRequestService spotifyRequestService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final SpotifyCollectionService spotifyCollectionService;
    private final RedisQueueService redisQueueService;
    private final RedisService redisService;
    private final SpotifyHarvestService spotifyHarvestService;

    @Autowired
    public SpotifyController(SpotifyTokenService spotifyTokenService,
                             SpotifyRequestService spotifyRequestService,
                             ServerAccessTokenService serverAccessTokenService,
                             SpotifyCollectionService spotifyCollectionService,
                             RedisQueueService redisQueueService, SpotifyHarvestService spotifyHarvestService,
                             RedisService redisService) {
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyRequestService = spotifyRequestService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.spotifyCollectionService = spotifyCollectionService;
        this.redisQueueService = redisQueueService;
        this.redisService = redisService;
        this.spotifyHarvestService = spotifyHarvestService;
    }

    // ----------------------------------------Auth Routes----------------------------------------
    @GetMapping("/authorize")
    public HashMap<String, String> getCode(@CookieValue("accessToken") String accessToken) throws NoSuchAlgorithmException {
        String authorizationUrl = spotifyTokenService.getAuthorizationUrl(accessToken);
        HashMap<String, String> result = new HashMap<>();
        result.put("authorizationUrl", authorizationUrl);   
        return result;
    }

    @GetMapping("/callback")
    public void getToken(@RequestParam("code") String code, @RequestParam("state") String receivedState, HttpServletResponse response) throws IOException {

        try {
            spotifyTokenService.handleSpotifyCallback(code, receivedState);
            response.sendRedirect(frontendUrl + "/spotify-connected");
        } catch (IllegalArgumentException e) {
            response.sendRedirect( frontendUrl + "/error?message=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<HashMap> verifyConnection(@CookieValue("accessToken") String accessToken) {
        HashMap result = new HashMap();
        result.put("connected?", spotifyTokenService.verifyConnection(accessToken));

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/remove-connection")
    public void removeConnection(@CookieValue("accessToken") String accessToken) {
        spotifyTokenService.removeConnection(accessToken);

        // Deleting queues
        ArrayList<String> types = new ArrayList<>();
        types.add("track");
        types.add("album");
        types.add("artist");
        for (String type : types) {
            redisService.deleteKey(redisQueueService.formatQueueKey(serverAccessTokenService.getClaimsFromToken(accessToken).getSubject(), type));
        }
    }


    // ----------------------------------------Spotify API Routes----------------------------------------
    @GetMapping("/random-track")
    public ResponseEntity<HashMap> getRandomTrack(@CookieValue("accessToken") String accessToken, HttpServletResponse response) {
        String username = serverAccessTokenService.getClaimsFromToken(accessToken).getSubject();

        HashMap randomTrack = makeRandomSearch(username, "track");

        spotifyCollectionService.createAndSaveTrackWithAlbumAndArtists(randomTrack);

        return ResponseEntity.ok(randomTrack);
    }

    @GetMapping("/random-album")
    public ResponseEntity<HashMap> getRandomAlbum(@CookieValue("accessToken") String accessToken) {
        String username = serverAccessTokenService.getClaimsFromToken(accessToken).getSubject();

        HashMap randomAlbum = makeRandomAlbumSearch(username);

        return ResponseEntity.ok(randomAlbum);
    }

    @GetMapping("/random-artist")
    public ResponseEntity<HashMap> getRandomArtist(@CookieValue("accessToken") String accessToken, HttpServletResponse response) {
        String username = serverAccessTokenService.getClaimsFromToken(accessToken).getSubject();

        HashMap randomArtist = makeRandomSearch(username, "artist");

        return ResponseEntity.ok(randomArtist);
    }

    @GetMapping("/test")
    public String test(@CookieValue("accessToken") String accessToken) {

        return "hello " + serverAccessTokenService.getClaimsFromToken(accessToken).getSubject();
    }

    // ----------------------------------------Private Methods----------------------------------------

    private HashMap makeRandomSearch(String username, String type) {
        int limit = 1;
        int offset = 0;

        // Pop from queue to get random string and total results
        //  and immediately async harvest a new track
        ArrayList<String> poppedItem = redisQueueService.popFromQueue(username, type);
        spotifyHarvestService.harvestOne(username, type);

        // Generate a random offset based on the total results
        Random random = new Random();
        offset = random.nextInt(Integer.parseInt(poppedItem.get(1)));

        HashMap spotifyResponse = spotifyRequestService.search(poppedItem.get(0), type, limit, offset, "audio", username);

        HashMap result = (HashMap) spotifyResponse.get(type + "s");
        ArrayList items = (ArrayList) result.get("items");

        // Theoretically, there shouldn't be 0 items as the queue should only be populated with the strings with results
        //  Unless in between the time the queue was populated and the time the user makes a request, Spotify removes items
        //  and the user happened to get a random offset greater than the total items
        if (items.isEmpty()) {
            return null;
        }

        return (HashMap) items.getFirst();
    }

    // Needs to be separate because of the way the Spotify API returns singles and compilations as albums
    private HashMap makeRandomAlbumSearch(String username) {
        Random random = new Random();
        ArrayList items = new ArrayList();
        final int limit = 50;

        while (items.isEmpty()) {
            int offset = 0;

            // Pop from queue to get random string and total results
            //  and immediately async harvest a new track
            ArrayList<String> poppedItem = redisQueueService.popFromQueue(username, "album");
            spotifyHarvestService.harvestOne(username, "album");

            int totalResults = Integer.parseInt(poppedItem.get(1));

            // Generate a random offset based on the total results
            if (totalResults > limit) {
                offset = random.nextInt(totalResults - (limit - 1));
            }

            HashMap spotifyResponse = spotifyRequestService.search(poppedItem.get(0), "album", limit, offset, "audio", username);
            HashMap result = (HashMap) spotifyResponse.get("albums");
            items = (ArrayList) result.get("items");

            // Removing all items that are not actual albums
            items.removeIf(item -> !((HashMap) item).get("album_type").equals("album"));
        }

        return (HashMap) items.get(random.nextInt(items.size()));
    }
}