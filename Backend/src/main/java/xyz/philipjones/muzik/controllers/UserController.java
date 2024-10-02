package xyz.philipjones.muzik.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.philipjones.muzik.services.UserService;
import xyz.philipjones.muzik.utils.PKCEUtil;
import xyz.philipjones.muzik.utils.ResponseHandler;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    String codeVerifier = PKCEUtil.generateCodeVerifier(128);

    @Value("${CLIENT_ID}")
    private String clientId;

    @Value("${REDIRECT_URI}")
    private String redirectUri;

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public void getCode(HttpServletResponse response) throws IOException, NoSuchAlgorithmException {
        String state = new BigInteger(130, new SecureRandom()).toString(32);
        String scope = "user-read-private user-read-email"; // Add the scopes you need
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

        String authorizationUrl = "https://accounts.spotify.com/authorize" +
                "?response_type=code" +
                "&client_id=" + this.clientId +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(this.redirectUri, StandardCharsets.UTF_8) +
                "&state=" + state +
                "&code_challenge=" + codeChallenge +
                "&code_challenge_method=S256";

        response.sendRedirect(authorizationUrl);
    }

    @GetMapping("/spotify/callback")
    public void getToken(@RequestParam("code") String code, HttpServletResponse clientResponse) throws IOException {
        final String tokenUrl = "https://accounts.spotify.com/api/token";

        // Create Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=authorization_code" +
                                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                                "&redirect_uri=" + URLEncoder.encode(this.redirectUri, StandardCharsets.UTF_8) +
                                "&client_id=" + URLEncoder.encode(this.clientId, StandardCharsets.UTF_8) +
                                "&code_verifier=" + URLEncoder.encode(this.codeVerifier, StandardCharsets.UTF_8)
                ))
                .build();

        // Send Request
        HttpClient client = HttpClient.newHttpClient();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    try {
                        ResponseHandler.handleResponse(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // Redirect to the frontend
        clientResponse.sendRedirect("https://www.bk.com/");
    }
}