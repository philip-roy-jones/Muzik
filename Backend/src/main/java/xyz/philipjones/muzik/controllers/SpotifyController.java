package xyz.philipjones.muzik.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.UnicodeScript;
import xyz.philipjones.muzik.services.RandomStringErrorService;
import xyz.philipjones.muzik.services.UnicodeScriptService;
import xyz.philipjones.muzik.services.spotify.SpotifyRequestService;
import xyz.philipjones.muzik.services.spotify.SpotifyTokenService;
import xyz.philipjones.muzik.services.RandomStringService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/spotify")
public class SpotifyController {

    private final RandomStringService randomStringService;
    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyRequestService spotifyRequestService;
    private final RandomStringErrorService randomStringErrorService;
    private final UnicodeScriptService unicodeScriptService;

    @Autowired
    public SpotifyController(RandomStringService randomStringService, SpotifyTokenService spotifyTokenService,
                             SpotifyRequestService spotifyRequestService,
                             RandomStringErrorService randomStringErrorService,
                             UnicodeScriptService unicodeScriptService) {
        this.randomStringService = randomStringService;
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyRequestService = spotifyRequestService;
        this.randomStringErrorService = randomStringErrorService;
        this.unicodeScriptService = unicodeScriptService;
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
    public HashMap<String, Object> getRandomTrack(@RequestHeader("Authorization") String authorizationHeader) {
        UnicodeScript unicodeScript = unicodeScriptService.generateRandomScript();

        String randomString = randomStringService.generateRandomString(unicodeScript);
        System.out.println("Random String: " + '|' + randomString + '|');
        int limit = 1;
        int offset = 0;

        HashMap spotifyResponse = spotifyRequestService.search(randomString, "track", limit, offset, "audio", authorizationHeader.substring("Bearer ".length()));

//        TODO: This randomizes it even further by making a second request using same args,
//          but using a random offset if the total results are greater than the limit.
//          Cannot implement at this current moment due to performance issues.
//        HashMap trackies = (HashMap) spotifyResponse.get("tracks");
//        int totalResults = (Integer) trackies.get("total");
//        System.out.println("Total Results: " + totalResults);
//
//        if (totalResults > limit) {
//            offset = new java.util.Random().nextInt(totalResults - limit);
//            System.out.println("Offset: " + offset);
//            spotifyResponse = spotifyService.search(randomString, "track", limit, offset, "audio", authorizationHeader.substring("Bearer ".length()));
//        }

        HashMap tracks = (HashMap) spotifyResponse.get("tracks");
        int totalResults = (Integer) tracks.get("total");
        ArrayList items = (ArrayList) tracks.get("items");

        System.out.println("Total Results: " + totalResults);
        while (totalResults == 0) {
            randomStringErrorService.saveRandomStringError(randomString);

            unicodeScript = unicodeScriptService.generateRandomScript();
            randomString = randomStringService.generateRandomString(unicodeScript);
            spotifyResponse = spotifyRequestService.search(randomString, "track", limit, offset, "audio", authorizationHeader.substring("Bearer ".length()));
            tracks = (HashMap) spotifyResponse.get("tracks");
            items = (ArrayList) tracks.get("items");
            totalResults = (Integer) tracks.get("total");

            if(totalResults != 0) {
                System.out.println("No Longer 0: " + '|' + randomString + '|');
                System.out.println("New Results: " + totalResults);
            }
        }

        HashMap randomTrack = (HashMap) items.getFirst();
//        System.out.println(randomTrack);
        String trackId = (String) randomTrack.get("id");
//        System.out.println(trackId);
        // TODO: Add that random track to the database asynchronously

        return spotifyResponse;
    }

    @GetMapping("/test")
    public String test(@RequestHeader("Authorization") String authorizationHeader) {

        return "hello world";
    }
}