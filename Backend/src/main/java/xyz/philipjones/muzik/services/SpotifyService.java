package xyz.philipjones.muzik.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.utils.PKCEUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

@Service
public class SpotifyService {

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.redirect-uri}")
    private String redirectUri;

    private final String codeVerifier = PKCEUtil.generateCodeVerifier(128);
    private final String state = new BigInteger(130, new SecureRandom()).toString(32);

    public String getAuthorizationUrl() throws NoSuchAlgorithmException {
        String scope = "user-read-private user-read-email playlist-read-collaborative";
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

        return "https://accounts.spotify.com/authorize" + "?response_type=code" + "&client_id=" + this.clientId + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) + "&redirect_uri=" + URLEncoder.encode(this.redirectUri, StandardCharsets.UTF_8) + "&state=" + this.state + "&code_challenge=" + codeChallenge + "&code_challenge_method=S256";
    }

    public HashMap handleSpotifyCallback(String code, String receivedState) throws IOException {
        if (!receivedState.equals(this.state)) {
            throw new IllegalArgumentException("State mismatch");
        }

        final String tokenUrl = "https://accounts.spotify.com/api/token";

        // Build POST request to get access token
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=authorization_code" +
                                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                                "&redirect_uri=" + URLEncoder.encode(this.redirectUri, StandardCharsets.UTF_8) +
                                "&client_id=" + URLEncoder.encode(this.clientId, StandardCharsets.UTF_8) +
                                "&code_verifier=" + URLEncoder.encode(this.codeVerifier, StandardCharsets.UTF_8)))
                .build();

        // Send request asynchronously and return the response body as a HashMap
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        return responseFuture.thenApply(response -> {
            try {
                return objectMapper.readValue(response.body(), HashMap.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).join();
    }

    public HashMap search(String query, String type, int limit, String includeExternal, String token) {
        // Build GET request to search for tracks
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.spotify.com/v1/search" + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=" + type + "&limit=" + limit + "&include_external=" + includeExternal)).header("Authorization", "Bearer " + token).header("Accept", "application/json").GET().build();

        // Send request asynchronously
        HttpClient client = HttpClient.newHttpClient();
        try {
            CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            return responseFuture.thenApply(response -> {
                try {
                    return objectMapper.readValue(response.body(), HashMap.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).join();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}