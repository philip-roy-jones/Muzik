package xyz.philipjones.muzik.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.philipjones.muzik.services.SpotifyService;
import xyz.philipjones.muzik.services.StringRandomService;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/spotify")
public class SpotifyController {

    private final StringRandomService stringRandomService;
    private final SpotifyService spotifyService;

    @Autowired
    public SpotifyController(StringRandomService stringRandomService, SpotifyService spotifyService) {
        this.stringRandomService = stringRandomService;
        this.spotifyService = spotifyService;
    }

    @GetMapping("/authorize")
    public String getCode(HttpServletResponse response) throws IOException, NoSuchAlgorithmException {
        String authorizationUrl = spotifyService.getAuthorizationUrl();
//        response.sendRedirect(authorizationUrl);
        return authorizationUrl;
    }

    @GetMapping("/callback")
    public void getToken(@RequestParam("code") String code, @RequestParam("state") String receivedState, HttpServletResponse clientResponse) throws IOException {
        try {
            HashMap response = spotifyService.handleSpotifyCallback(code, receivedState);
            // Store refresh token in MongoDB

            // Store access token in Redis

            clientResponse.sendRedirect("https://www.bk.com/");
        } catch (IllegalArgumentException e) {
            clientResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/random-track")
    public HashMap<String, Object> getRandomTrack() {
        String randomString = this.stringRandomService.generateRandomString();

        return spotifyService.search("runaway", "track",1,"audio","BQAIjQ-7knUxobtrGJoE-fXIhrBODVBG1Rh1Aiy2zq53xyPA6yjKFdhsKKaQsmkoIYgK-ncnhqbJ7UOPMWWx_a2ehPxA3aPnqxx_odQoDYN3-oCZCxc");
    }

    @GetMapping("/test")
    public String test() {
        return "Hello World from Spotify Controller!";
    }
}