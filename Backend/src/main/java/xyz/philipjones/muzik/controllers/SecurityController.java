package xyz.philipjones.muzik.controllers;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.security.*;
// TODO: Remove this import below, use the service instead
import xyz.philipjones.muzik.services.security.ExternalAccessTokenService;
import xyz.philipjones.muzik.services.security.AuthenticationService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.security.ServerRefreshTokenService;
import xyz.philipjones.muzik.services.security.UserService;
import xyz.philipjones.muzik.services.spotify.SpotifyTokenService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

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

            if (!authentication.isAuthenticated()) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot authenticate user");
                return ResponseEntity.status(500).body(errorResponse);
            }

            String username = authentication.getName();

            List<UserRole> roles = userService.getRolesByUsername(username);

            String accessToken = serverAccessTokenService.generateAccessToken(username, roles);
            ServerRefreshToken refreshTokenObj = serverRefreshTokenService.generateRefreshToken(username, loginRequest.isRememberMe(), accessToken);

            // Caching the most recent access token in Redis
            serverAccessTokenService.cacheAccessToken(serverAccessTokenService.getClaimsFromToken(accessToken).getId());

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

    @GetMapping("/check")       // Check authentication and authorization
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
            HashMap<String, String> renewedTokens = tokenRenewal(refreshToken);
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

        // Clear access token cache
        if (System.currentTimeMillis() < accessTokenExpiry.getTime()) {
            serverAccessTokenService.clearAccessTokenCache(jti);
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
        List<UserRole> roles = userService.getRolesByUsername(username);
        Date oldRefreshExpiry = refreshTokenObj.getExpiryDate();

        // Generate new refresh and access token
        String newAccessToken = serverAccessTokenService.generateAccessToken(username, roles);
        ServerRefreshToken newRefreshTokenObj = serverRefreshTokenService
                .generateRefreshTokenWithExpiryDate(username, oldRefreshExpiry, newAccessToken);

        if (newRefreshTokenObj == null) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Refresh token expired, please login again");
            return errorResponse;
        }

        // Caching the most recent access token in Redis
        Claims newClaims = serverAccessTokenService.getClaimsFromToken(newAccessToken);
        serverAccessTokenService.cacheAccessToken(newClaims.getId());

        // Clear the old access token cache
        serverAccessTokenService.clearAccessTokenCache(refreshTokenObj);

        // Delete the old refresh token
        serverRefreshTokenService.deleteRefreshToken(refreshTokenObj);

        return new HashMap<String, String>() {{
            put("accessToken", newAccessToken);
            put("refreshToken", serverRefreshTokenService.getRefreshToken(newRefreshTokenObj));
        }};
    }

    private void setRefreshTokenCookie(ServerRefreshToken refreshTokenObj, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refreshToken",
                refreshTokenObj != null ? serverRefreshTokenService.getRefreshToken(refreshTokenObj) : "");

        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) ((refreshTokenObj.getExpiryDate().getTime() - System.currentTimeMillis()) / 1000)); // In Seconds
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