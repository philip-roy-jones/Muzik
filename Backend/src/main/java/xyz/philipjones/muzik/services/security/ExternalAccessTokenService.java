package xyz.philipjones.muzik.services.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.services.spotify.SpotifyTokenService;

import java.io.IOException;

@Service
public class ExternalAccessTokenService {

    private final SpotifyTokenService spotifyTokenService;
    private final UserService userService;

    @Autowired
    public ExternalAccessTokenService(SpotifyTokenService spotifyTokenService, UserService userService) {
        this.spotifyTokenService = spotifyTokenService;
        this.userService = userService;
    }

    public void refreshAllTokens(String username) {
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Grab refresh tokens here
        String spotifyRefreshToken = userService.getSpotifyRefreshToken(user);
//        String googleRefreshToken = userService.getGoogleRefreshToken(user);

        // Check refresh tokens here
        if (spotifyRefreshToken != null) {
            try {
                if (spotifyTokenService.refreshAccessToken(spotifyRefreshToken, user)) {
//                    System.out.println("Successfully refreshed Spotify access token");
                };
            } catch (IOException e) {
                // Handle the exception, e.g., log it or rethrow it as a runtime exception
                throw new RuntimeException("Failed to refresh Spotify access token", e);
            }
        } else {
            System.out.println("No Spotify refresh token found");
        }

//        if (googleRefreshToken != null) {
//            // Refresh Google access token here
//        }
    }
}
