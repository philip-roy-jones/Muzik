package xyz.philipjones.muzik.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import xyz.philipjones.muzik.models.LoginRequest;
import xyz.philipjones.muzik.models.RegistrationRequest;
import xyz.philipjones.muzik.models.User;
import xyz.philipjones.muzik.services.AccessTokenService;
import xyz.philipjones.muzik.services.UserService;

import java.util.Date;

@RestController
@RequestMapping("/public")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AccessTokenService accessTokenService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserService userService, AccessTokenService accessTokenService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.accessTokenService = accessTokenService;
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
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            String token = accessTokenService.generateToken(authentication);
            return ResponseEntity.ok(token);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid login credentials");
        }
    }
}