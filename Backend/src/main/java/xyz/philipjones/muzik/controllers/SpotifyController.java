package xyz.philipjones.muzik.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletResponse;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.services.SpotifyService;
import xyz.philipjones.muzik.services.SpotifyTokenService;
import xyz.philipjones.muzik.services.StringRandomService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.security.UserService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/spotify")
public class SpotifyController {

    private final StringRandomService stringRandomService;
    private final SpotifyTokenService spotifyTokenService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final SpotifyService spotifyService;
    private final UserService userService;

    @Autowired
    public SpotifyController(StringRandomService stringRandomService, SpotifyTokenService spotifyTokenService,
                             ServerAccessTokenService serverAccessTokenService, SpotifyService spotifyService,
                             UserService userService) {
        this.stringRandomService = stringRandomService;
        this.spotifyTokenService = spotifyTokenService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.spotifyService = spotifyService;
        this.userService = userService;
    }

    @GetMapping("/authorize")
    public HashMap<String, String> getCode(@RequestHeader("Authorization") String authorizationHeader) throws NoSuchAlgorithmException {
        String authorizationUrl = spotifyTokenService.getAuthorizationUrl(authorizationHeader.substring("Bearer ".length()));
        HashMap<String, String> result = new HashMap<>();
        result.put("authorizationUrl", authorizationUrl);
        return result;
    }

    @GetMapping("/callback")
    public HashMap<String, String> getToken(@RequestParam("code") String code, @RequestParam("state") String receivedState, HttpServletResponse clientResponse) throws IOException {
        try {
            return spotifyTokenService.handleSpotifyCallback(code, receivedState);
        } catch (IllegalArgumentException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    @GetMapping("/refresh")
    public HashMap<String, String> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        // Using an array to store the response so we can modify it in the lambda
        final HashMap[] response = {new HashMap()};

        String username = serverAccessTokenService.getClaimsFromToken(authorizationHeader.substring("Bearer ".length())).getSubject();
        User user = userService.getUserByUsername(username).orElse(null);
        if (user != null) {
            String refreshToken = userService.getSpotifyRefreshToken(user);

            try {
                response[0] = spotifyTokenService.refreshAccessToken(refreshToken, user);
            } catch (IllegalArgumentException e) {
                response[0].put("error", e.getMessage());
            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        return response[0];
    }

    @GetMapping("/random-track")
    public HashMap<String, Object> getRandomTrack(@RequestHeader("Authorization") String authorizationHeader) {
        String randomString = this.stringRandomService.generateRandomString();
        HashMap spotifyResponse = spotifyService.search("runaway", "track", 50, "audio", authorizationHeader.substring("Bearer ".length()));
        return spotifyResponse;
    }

    @GetMapping("/test")
    public String test() {
        return "Hello from the Spotify Controller!";
    }
}