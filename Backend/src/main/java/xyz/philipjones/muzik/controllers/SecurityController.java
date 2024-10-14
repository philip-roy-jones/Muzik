package xyz.philipjones.muzik.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.security.LoginRequest;
import xyz.philipjones.muzik.models.security.RegistrationRequest;
import xyz.philipjones.muzik.models.security.ServerRefreshToken;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.services.security.AuthenticationService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.security.ServerRefreshTokenService;
import xyz.philipjones.muzik.services.security.UserService;

import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/public")
public class SecurityController {

    private final UserService userService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final ServerRefreshTokenService serverRefreshTokenService;
    private final AuthenticationService authenticationService;

    @Autowired
    public SecurityController(UserService userService, ServerAccessTokenService serverAccessTokenService,
                              ServerRefreshTokenService serverRefreshTokenService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.serverRefreshTokenService = serverRefreshTokenService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistrationRequest registrationRequest) {
        try {
            User user = new User();
            user.setUsername(registrationRequest.getUsername());
            user.setFirstName(registrationRequest.getFirstName());
            user.setLastName(registrationRequest.getLastName());
            user.setEmail(registrationRequest.getEmail());
            user.setPassword(registrationRequest.getPassword());
            user.setCreatedAt(new Date());
            user.setUpdatedAt(new Date());

            if (userService.registerUser(user)) {
                return ResponseEntity.ok("Registration successful");
            } else {
                return ResponseEntity.status(400).body("Username already exists");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during registration");
        }
    }

    @PostMapping("/login")          // TODO: Move functionality to loginUser in UserService
    public ResponseEntity<HashMap> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationService.authenticate(loginRequest);
            if (!authentication.isAuthenticated()) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot authenticate user");
                return ResponseEntity.status(500).body(errorResponse);
            }

            String accessToken = serverAccessTokenService.generateAccessToken(authentication.getName());
            ServerRefreshToken refreshToken = serverRefreshTokenService.generateRefreshToken(authentication.getName(), loginRequest.isRememberMe(), accessToken);

            HashMap<String, String> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", serverRefreshTokenService.getRefreshToken(refreshToken));

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid login credentials");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/refresh")                // TODO: Move functionality to UserService
    public ResponseEntity<HashMap> refresh(@RequestBody HashMap<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "No refresh token provided");
            return ResponseEntity.status(400).body(errorResponse);
        }

        if (!serverRefreshTokenService.validateRefreshToken(refreshToken)) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid refresh token");
            return ResponseEntity.status(401).body(errorResponse);
        }

        ServerRefreshToken refreshTokenObj = serverRefreshTokenService.findByToken(serverRefreshTokenService.encryptRefreshToken(refreshToken));
        String accessToken = serverAccessTokenService.generateAccessToken(refreshTokenObj.getUsername());

        // Blacklist old access token
        serverAccessTokenService.blacklistAccessToken(refreshTokenObj.getAccessJti());

        // Update jti in database
        refreshTokenObj.setAccessJti(serverAccessTokenService.encryptJti(serverAccessTokenService.getClaimsFromToken(accessToken)));
        serverRefreshTokenService.saveRefreshToken(refreshTokenObj);

        HashMap<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);

        return ResponseEntity.ok(response);
    }
}