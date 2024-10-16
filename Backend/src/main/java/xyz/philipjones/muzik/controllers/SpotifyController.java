package xyz.philipjones.muzik.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.services.SpotifyService;
import xyz.philipjones.muzik.services.SpotifyTokenService;
import xyz.philipjones.muzik.services.StringRandomService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/spotify")
public class SpotifyController {

    private final StringRandomService stringRandomService;
    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyService spotifyService;

    @Autowired
    public SpotifyController(StringRandomService stringRandomService, SpotifyTokenService spotifyTokenService,
                             SpotifyService spotifyService) {
        this.stringRandomService = stringRandomService;
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyService = spotifyService;
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
        String randomString = this.stringRandomService.generateRandomString();
        HashMap spotifyResponse = spotifyService.search(randomString, "track", 50, "audio", authorizationHeader.substring("Bearer ".length()));
        // TODO: Choose a random track from the response and return it
        HashMap tracks = (HashMap) spotifyResponse.get("tracks");
        ArrayList items = (ArrayList) tracks.get("items");

        Integer itemSize = items.size();
        System.out.println("Item Size: " + itemSize);
        Integer randomIndex = new java.util.Random().nextInt(items.size());
        System.out.println("Random Index: " + randomIndex);
        HashMap randomTrack = (HashMap) items.get(randomIndex);
        System.out.println(randomTrack);
        String trackId = (String) randomTrack.get("id");
        System.out.println(trackId);
        // TODO: Add that random track to the database

        return spotifyResponse;
    }

    @GetMapping("/test")
    public String test() {
        return "Hello from the Spotify Controller!";
    }
}