package xyz.philipjones.muzik.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.security.LoginRequest;
import xyz.philipjones.muzik.models.security.RegistrationRequest;
// TODO: Remove this import below, use the service instead
import xyz.philipjones.muzik.models.security.ServerRefreshToken;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.services.security.ExternalAccessTokenService;
import xyz.philipjones.muzik.services.security.AuthenticationService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.security.ServerRefreshTokenService;
import xyz.philipjones.muzik.services.security.UserService;
import xyz.philipjones.muzik.services.spotify.SpotifyHarvestService;
import xyz.philipjones.muzik.services.spotify.SpotifyTokenService;

import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "${frontend.https.url}", allowCredentials = "true")      // Credentials are required for cookies to be sent and stored
public class SecurityController {

    private final UserService userService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final ServerRefreshTokenService serverRefreshTokenService;
    private final AuthenticationService authenticationService;
    private final ExternalAccessTokenService externalAccessTokenRefreshService;
    private final SpotifyHarvestService spotifyHarvestService;
    private final SpotifyTokenService spotifyTokenService;

    @Autowired
    public SecurityController(UserService userService, ServerAccessTokenService serverAccessTokenService,
                              ServerRefreshTokenService serverRefreshTokenService, AuthenticationService authenticationService,
                              ExternalAccessTokenService externalAccessTokenRefreshService,
                              SpotifyHarvestService spotifyHarvestService, SpotifyTokenService spotifyTokenService) {
        this.userService = userService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.serverRefreshTokenService = serverRefreshTokenService;
        this.authenticationService = authenticationService;
        this.externalAccessTokenRefreshService = externalAccessTokenRefreshService;
        this.spotifyHarvestService = spotifyHarvestService;
        this.spotifyTokenService = spotifyTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<HashMap> register(@RequestBody RegistrationRequest registrationRequest, HttpServletResponse response) {
        try {
            if (!registrationRequest.getPassword().equals(registrationRequest.getConfirmPassword())) {
                return ResponseEntity.status(400).body(new HashMap<String, String>() {{
                    put("error", "Passwords do not match");
                }});
            }

            User user = new User();
            user.setUsername(registrationRequest.getUsername());
            user.setEmail(registrationRequest.getEmail());
            user.setPassword(registrationRequest.getPassword());
            user.setCreatedAt(new Date());
            user.setUpdatedAt(new Date());

            if (userService.registerUser(user)) {
                // Authenticate user by calling login method
                LoginRequest loginRequest = new LoginRequest(registrationRequest.getUsername(),
                        registrationRequest.getPassword(), false);
                return login(loginRequest, response);
            } else {
                return ResponseEntity.status(400).body(new HashMap<String, String>() {{
                    put("error", "Username already exists");
                }});
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new HashMap<String, String>() {{
                put("error", "Error during registration");
            }});
        }
    }

    @PostMapping("/login")
    public ResponseEntity<HashMap> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationService.authenticate(loginRequest);
            if (!authentication.isAuthenticated()) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot authenticate user");
                return ResponseEntity.status(500).body(errorResponse);
            }

            String accessToken = serverAccessTokenService.generateAccessToken(authentication.getName());
            ServerRefreshToken refreshTokenObj = serverRefreshTokenService.generateRefreshToken(authentication.getName(), loginRequest.isRememberMe(), accessToken);

            refreshTokenAndInitQueue(refreshTokenObj);

            Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
            accessTokenCookie.setHttpOnly(false);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge((int) serverAccessTokenService.getAccessTokenExpirationInMs() / 1000); // In Seconds
            response.addCookie(accessTokenCookie);

            Cookie refreshTokenCookie = new Cookie("refreshToken", serverRefreshTokenService.getRefreshToken(refreshTokenObj));
            refreshTokenCookie.setHttpOnly(false);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge((int) ((refreshTokenObj.getExpiryDate().getTime() - System.currentTimeMillis()) / 1000)); // In Seconds
            response.addCookie(refreshTokenCookie);

            return ResponseEntity.ok(new HashMap<>());
        } catch (AuthenticationException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid login credentials");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<HashMap> refresh(@RequestBody HashMap<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "No refresh token provided");
            return ResponseEntity.status(400).body(errorResponse);
        }

        if (!serverRefreshTokenService.validateRefreshToken(refreshToken)) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid refresh token, please login again");
            return ResponseEntity.status(401).body(errorResponse);
        }

        ServerRefreshToken refreshTokenObj = serverRefreshTokenService.findByToken(serverRefreshTokenService.encryptRefreshToken(refreshToken));
        String accessToken = serverAccessTokenService.generateAccessToken(refreshTokenObj.getUsername());

        // Blacklist old access token if it hasn't expired
        if (System.currentTimeMillis() < refreshTokenObj.getAccessExpiryDate().getTime()) {
            serverAccessTokenService.blacklistAccessToken(refreshTokenObj.getAccessJti(), refreshTokenObj.getAccessExpiryDate());
        }

        // Update jti in database
        serverRefreshTokenService.setAccessJti(refreshTokenObj, accessToken);
        serverRefreshTokenService.setAccessExpiryDate(refreshTokenObj);
        serverRefreshTokenService.saveRefreshToken(refreshTokenObj);

        refreshTokenAndInitQueue(refreshTokenObj);

        HashMap<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);

        return ResponseEntity.ok(new HashMap<>());
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody HashMap<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (!serverRefreshTokenService.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body("Invalid refresh token, please login again");
        }

        ServerRefreshToken refreshTokenObj = serverRefreshTokenService.findByToken(serverRefreshTokenService.encryptRefreshToken(refreshToken));

        // Blacklist old access token if it hasn't expired
        if (System.currentTimeMillis() < refreshTokenObj.getAccessExpiryDate().getTime()) {
            serverAccessTokenService.blacklistAccessToken(refreshTokenObj.getAccessJti(), refreshTokenObj.getAccessExpiryDate());
        }

        // Delete external access tokens
        spotifyTokenService.deleteSpotifyAccessToken(refreshTokenObj.getUsername());

        // Remove refresh token from database
        serverRefreshTokenService.deleteRefreshToken(refreshTokenObj);

        return ResponseEntity.ok("Logout successful");
    }

    private void refreshTokenAndInitQueue(ServerRefreshToken refreshTokenObj) {
        externalAccessTokenRefreshService.refreshAllTokens(refreshTokenObj.getUsername());

        if (userService.getSpotifyRefreshToken(userService.getUserByUsername(refreshTokenObj.getUsername()).get()) != null) {
            spotifyHarvestService.initSpotifyQueues(refreshTokenObj.getUsername());
        }
    }
}