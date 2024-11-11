package xyz.philipjones.muzik.controllers;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.View;
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
import xyz.philipjones.muzik.services.spotify.SpotifyTokenService;

import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/public")
@CrossOrigin(origins = "${frontend.https.url}", allowCredentials = "true")
// Credentials are required for cookies to be sent and stored
public class SecurityController {

    private final UserService userService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final ServerRefreshTokenService serverRefreshTokenService;
    private final AuthenticationService authenticationService;
    private final ExternalAccessTokenService externalAccessTokenRefreshService;
    private final SpotifyTokenService spotifyTokenService;

    @Autowired
    public SecurityController(UserService userService, ServerAccessTokenService serverAccessTokenService,
                              ServerRefreshTokenService serverRefreshTokenService, AuthenticationService authenticationService,
                              ExternalAccessTokenService externalAccessTokenRefreshService,
                              SpotifyTokenService spotifyTokenService) {
        this.userService = userService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.serverRefreshTokenService = serverRefreshTokenService;
        this.authenticationService = authenticationService;
        this.externalAccessTokenRefreshService = externalAccessTokenRefreshService;
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
            System.out.println(authentication);
            // TODO: Get user object from authentication and pass it to generateAccessToken
            if (!authentication.isAuthenticated()) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot authenticate user");
                return ResponseEntity.status(500).body(errorResponse);
            }

            String accessToken = serverAccessTokenService.generateAccessToken(authentication.getName());
            ServerRefreshToken refreshTokenObj = serverRefreshTokenService.generateRefreshToken(authentication.getName(), loginRequest.isRememberMe(), accessToken);

            externalAccessTokenRefreshService.refreshAllTokens(refreshTokenObj.getUsername());

            setRefreshTokenCookie(refreshTokenObj, response);
            setAccessTokenCookie(accessToken, response);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("isLoggedIn", true);
                put("accessTokenExpiration", serverAccessTokenService.getAccessTokenExpirationInMs() / 1000);
            }});
        } catch (AuthenticationException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid login credentials");
            return ResponseEntity.status(200).body(errorResponse);
        }
    }

    @PostMapping("/refresh")    // Frontend refresh nearing expiration
    public ResponseEntity<HashMap> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
        ResponseEntity<HashMap> validationResponse = validateRefreshToken(refreshToken);
        if (validationResponse != null) return validationResponse;

        HashMap<String,String> renewedTokens = tokenRenewal(refreshToken);
        ServerRefreshToken newRefreshTokenObj = serverRefreshTokenService.findByToken(serverRefreshTokenService.encryptRefreshToken(renewedTokens.get("refreshToken")));

        // Refresh external access tokens
        externalAccessTokenRefreshService.refreshAllTokens(newRefreshTokenObj.getUsername());

        // Overwrite old refresh token with new refresh token
        setRefreshTokenCookie(newRefreshTokenObj, response);
        setAccessTokenCookie(renewedTokens.get("accessToken"), response);

        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("isLoggedIn", true);
            put("accessTokenExpiration", serverAccessTokenService.getAccessTokenExpirationInMs() / 1000);
        }});
    }

    @GetMapping("/check")       // Initial page load or refresh
    public ResponseEntity<HashMap> check(@CookieValue(value = "accessToken", required = false) String accessToken,
                                         @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                         HttpServletResponse response) {
        boolean accessTokenValid = accessToken != null && serverAccessTokenService.validateAccessToken(accessToken);
        boolean refreshTokenValid = refreshToken != null && serverRefreshTokenService.validateRefreshToken(refreshToken);

        if (accessTokenValid) {
            Claims claims = serverAccessTokenService.getClaimsFromToken(accessToken);
            long accessTokenExpiration = claims.getExpiration().getTime() - System.currentTimeMillis();

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("isLoggedIn", true);
                put("accessTokenExpiration", 1000000 / 1000);
            }});
        } else if (refreshTokenValid) {
            HashMap<String,String> renewedTokens = tokenRenewal(refreshToken);
            ServerRefreshToken refreshTokenObj = serverRefreshTokenService.findByToken(serverRefreshTokenService.encryptRefreshToken(renewedTokens.get("refreshToken")));
            if (refreshTokenObj == null) {
                return ResponseEntity.status(200).body(new HashMap<String, String>() {{
                    put("error", "Invalid refresh token, please login");
                }});
            }
            setRefreshTokenCookie(refreshTokenObj, response);
            setAccessTokenCookie(renewedTokens.get("accessToken"), response);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("isLoggedIn", true);
                put("accessTokenExpiration", serverAccessTokenService.getAccessTokenExpirationInMs() / 1000);
            }});
        } else {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("isLoggedIn", false);
                put("accessTokenExpiration", -1);
            }});
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<HashMap> logout(@CookieValue(value = "accessToken", required = false) String accessToken,
                                          @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                          HttpServletResponse response) {
        // TODO: Use refresh token cookie instead, access token does not function well when it has expired

        Claims accessTokenClaims = serverAccessTokenService.getClaimsFromToken(accessToken);
        String username = accessTokenClaims.getSubject();
        Date accessTokenExpiry = accessTokenClaims.getExpiration();
        String jti = accessTokenClaims.getId();

        // Blacklist old access token if it hasn't expired
        if (System.currentTimeMillis() < accessTokenExpiry.getTime()) {
            serverAccessTokenService.blacklistAccessToken(jti, accessTokenExpiry);
        }

        // Delete external access tokens
        spotifyTokenService.deleteSpotifyAccessToken(username);

        // Remove refresh token from database

        ServerRefreshToken refreshTokenObj = serverRefreshTokenService.findByAccessJti(jti);
        serverRefreshTokenService.deleteRefreshToken(refreshTokenObj);

        // Clear token cookie
        deleteTokenCookie("refreshToken", response);
        deleteTokenCookie("accessToken", response);

        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("message", "success");
        }});
    }

    private HashMap<String, String> tokenRenewal(String refreshToken) {
        // Grabbing attributes from refresh token
        ServerRefreshToken refreshTokenObj = serverRefreshTokenService.findByToken(serverRefreshTokenService.encryptRefreshToken(refreshToken));
        String username = refreshTokenObj.getUsername();
        Date oldRefreshExpiry = refreshTokenObj.getExpiryDate();

        // Generate new refresh and access token
        String accessToken = serverAccessTokenService.generateAccessToken(username);
        ServerRefreshToken newRefreshTokenObj = serverRefreshTokenService
                .generateRefreshTokenWithExpiryDate(username, oldRefreshExpiry, accessToken);

        if (newRefreshTokenObj == null) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Refresh token expired, please login again");
            return errorResponse;
        }

        // Blacklist old access token if it hasn't expired
        if (System.currentTimeMillis() < refreshTokenObj.getAccessExpiryDate().getTime()) {
            serverAccessTokenService.blacklistAccessToken(refreshTokenObj.getAccessJti(), refreshTokenObj.getAccessExpiryDate());
        }

        // Delete the old refresh token
        serverRefreshTokenService.deleteRefreshToken(refreshTokenObj);

        return new HashMap<String, String>() {{
            put("accessToken", accessToken);
            put("refreshToken", serverRefreshTokenService.getRefreshToken(newRefreshTokenObj));
        }};
    }

    private void setRefreshTokenCookie(ServerRefreshToken refreshTokenObj, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refreshToken",
                refreshTokenObj != null ? serverRefreshTokenService.getRefreshToken(refreshTokenObj) : "");

        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) ((refreshTokenObj.getExpiryDate().getTime() - System.currentTimeMillis())/1000)); // In Seconds
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);
    }

    private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);

        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (serverAccessTokenService.getAccessTokenExpirationInMs()) / 1000); // In Seconds
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessTokenCookie);
    }

    private void deleteTokenCookie(String type, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie(type, "");

        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(-1); // Setting negative max age deletes the cookie
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);
    }

    private ResponseEntity<HashMap> validateRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "No refresh token provided");
            return ResponseEntity.status(200).body(errorResponse);
        } else if (!serverRefreshTokenService.validateRefreshToken(refreshToken)) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid refresh token, please login");
            return ResponseEntity.status(200).body(errorResponse);
        }
        return null;
    }
}