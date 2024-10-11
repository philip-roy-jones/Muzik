package xyz.philipjones.muzik.controllers;

import io.jsonwebtoken.Jwts;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.security.LoginRequest;
import xyz.philipjones.muzik.models.security.RegistrationRequest;
import xyz.philipjones.muzik.models.security.ServerRefreshToken;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.repositories.ServerRefreshTokenRepository;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.security.ServerRefreshTokenService;
import xyz.philipjones.muzik.services.security.UserService;
import xyz.philipjones.muzik.utils.JwtKeyProvider;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;

@RestController
@RequestMapping("/public")
public class SecurityController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final ServerRefreshTokenService serverRefreshTokenService;
    private final StringEncryptor stringEncryptor;
    private final ServerRefreshTokenRepository serverRefreshTokenRepository;
    private final Key key;

    @Autowired
    public SecurityController(AuthenticationManager authenticationManager,
                              UserService userService,
                              ServerAccessTokenService serverAccessTokenService,
                              ServerRefreshTokenService serverRefreshTokenService,
                              @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                              ServerRefreshTokenRepository serverRefreshTokenRepository,
                              JwtKeyProvider jwtKeyProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.serverRefreshTokenService = serverRefreshTokenService;
        this.stringEncryptor = stringEncryptor;
        this.serverRefreshTokenRepository = serverRefreshTokenRepository;
        this.key = jwtKeyProvider.getKey();
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
                return ResponseEntity.status(400).body("Invalid registration request");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error during registration");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<HashMap> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            if (!authentication.isAuthenticated()) {
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot authenticate user");
                return ResponseEntity.status(500).body(errorResponse);
            }

            String accessToken = serverAccessTokenService.generateAccessToken(authentication.getName());
            ServerRefreshToken refreshToken = serverRefreshTokenService.generateRefreshToken(authentication.getName(), loginRequest.isRememberMe(), accessToken);

            HashMap<String, String> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", stringEncryptor.decrypt(refreshToken.getToken()));

            return ResponseEntity.ok(response);
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
            errorResponse.put("error", "Invalid refresh token");
            return ResponseEntity.status(401).body(errorResponse);
        }

        ServerRefreshToken refreshTokenObj = serverRefreshTokenRepository.findByToken(stringEncryptor.encrypt(refreshToken)).orElseThrow(() -> new RuntimeException("Refresh token not found"));
        String accessToken = serverAccessTokenService.generateAccessToken(refreshTokenObj.getUsername());

        // Blacklist old access token
        serverAccessTokenService.blacklistAccessToken(refreshTokenObj.getAccessJti());

        // Update jti in database
        refreshTokenObj.setAccessJti(stringEncryptor.encrypt(Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getBody().getId()));
        serverRefreshTokenRepository.save(refreshTokenObj);

        HashMap<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);

        return ResponseEntity.ok(response);
    }
}