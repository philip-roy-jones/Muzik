package xyz.philipjones.muzik.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.services.SpotifyService;
import xyz.philipjones.muzik.services.SpotifyTokenService;
import xyz.philipjones.muzik.services.StringRandomService;
import xyz.philipjones.muzik.services.security.UserService;

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
    private final UserService userService;

    @Autowired
    public SpotifyController(StringRandomService stringRandomService, SpotifyTokenService spotifyTokenService,
                             SpotifyService spotifyService, UserService userService) {
        this.stringRandomService = stringRandomService;
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyService = spotifyService;
        this.userService = userService;
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
        String randomString = stringRandomService.generateRandomString();
        int limit = 1;
        int offset = 0;

        HashMap spotifyResponse = spotifyService.search(randomString, "track", limit, offset, "audio", authorizationHeader.substring("Bearer ".length()));

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
        ArrayList items = (ArrayList) tracks.get("items");
        Integer itemSize = items.size();

        while (itemSize == 0) {
        // TODO: Record the random string that was used to search for the track for debug purposes
            System.out.println("Item Size: " + itemSize);
            randomString = stringRandomService.generateRandomString();
            spotifyResponse = spotifyService.search(randomString, "track", limit, offset, "audio", authorizationHeader.substring("Bearer ".length()));
            tracks = (HashMap) spotifyResponse.get("tracks");
            items = (ArrayList) tracks.get("items");
            itemSize = items.size();
        }

        HashMap randomTrack = (HashMap) items.getFirst();
        System.out.println(randomTrack);
        String trackId = (String) randomTrack.get("id");
        System.out.println(trackId);
        // TODO: Add that random track to the database asynchronously

        return spotifyResponse;
    }

    @GetMapping("/test")
    public String test(@RequestHeader("Authorization") String authorizationHeader) {

        return "FUCK";
    }
}