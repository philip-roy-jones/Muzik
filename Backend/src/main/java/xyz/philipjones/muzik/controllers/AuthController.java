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
import xyz.philipjones.muzik.services.UserService;

import java.util.Date;

@RestController
@RequestMapping("/public")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
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
            System.out.println(user.getUsername());
            System.out.println(user.getFirstName());
            System.out.println(user.getLastName());
            System.out.println(user.getEmail());
            System.out.println(user.getPassword());
            System.out.println(user.getCreatedAt());
            boolean result = userService.registerUser(user);
            System.out.println(result);
            if (result) {
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
            System.out.println("Testicle2");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            System.out.println("Testicle3");
            return ResponseEntity.ok("Login successful");
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid login credentials");
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Test";
    }
}