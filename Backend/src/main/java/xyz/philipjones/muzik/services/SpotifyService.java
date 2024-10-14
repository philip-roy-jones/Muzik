package xyz.philipjones.muzik.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.repositories.UserRepository;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.utils.PKCEUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SpotifyService {

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.redirect-uri}")
    private String redirectUri;

    private final String codeVerifier = PKCEUtil.generateCodeVerifier(128);
    private final String state = new BigInteger(130, new SecureRandom()).toString(32);

    private final RedisTemplate<String, String> redisTemplate;
    private final ServerAccessTokenService serverAccessTokenService;
    private final UserRepository userRepository;
    private final StringEncryptor stringEncryptor;

    @Autowired
    public SpotifyService(RedisTemplate<String, String> redisTemplate, ServerAccessTokenService serverAccessTokenService,
                          UserRepository userRepository, @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.redisTemplate = redisTemplate;
        this.serverAccessTokenService = serverAccessTokenService;
        this.userRepository = userRepository;
        this.stringEncryptor = stringEncryptor;
    }

    public String getAuthorizationUrl(String serverAccessToken) throws NoSuchAlgorithmException {
        String userId = userRepository.findByUsername(serverAccessTokenService.getClaimsFromToken(serverAccessToken).getSubject())
                .map(user -> user.getId().toString()).orElseThrow(() -> new RuntimeException("User not found"));

        String scope = "user-read-private user-read-email playlist-read-collaborative";
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

        redisTemplate.opsForValue().set("spotifyState:" + this.state, userId, 10, TimeUnit.MINUTES);

        return "https://accounts.spotify.com/authorize" + "?response_type=code" + "&client_id=" + this.clientId + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) + "&redirect_uri=" + URLEncoder.encode(this.redirectUri, StandardCharsets.UTF_8) + "&state=" + this.state + "&code_challenge=" + codeChallenge + "&code_challenge_method=S256";
    }

    public HashMap handleSpotifyCallback(String code, String receivedState) throws IOException {
        // TODO: Needs abstraction
        HashMap<String, String> result = new HashMap<>();

        if (!receivedState.equals(this.state)) {
            result.put("error", "State mismatch");
        }

        String storedStateStr = redisTemplate.opsForValue().get("spotifyState:" + receivedState); // Get the user ID from the state
        ObjectId storedState = storedStateStr != null ? new ObjectId(storedStateStr) : null; // Convert the user ID to an ObjectId
        if (storedState == null) {
            result.put("error", "State not found");
            return result;
        }

        redisTemplate.delete("spotifyState:" + receivedState); // Delete the state from Redis since we have found it

        User user = userRepository.findById(storedState).orElse(null);
        if (user == null) {
            result.put("error", "User not found");
            return result;
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

        // Send request synchronously and return the response body as a HashMap
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new IOException("Request interrupted", e);
        }

        // Handle response

        HashMap responseBody = new ObjectMapper().readValue(response.body(), HashMap.class);
        // Store refresh token in MongoDB
        HashMap connection = new HashMap();
        connection.put("refreshToken", stringEncryptor.encrypt(responseBody.get("refresh_token").toString()));
        connection.put("issueDate", new Date());
        System.out.println("Refresh Token: " + responseBody.get("refresh_token").toString());
        user.addConnection("spotify", connection);
        userRepository.save(user);

        // Store access token in Redis
        redisTemplate.opsForValue().set("spotifyAccessToken:" + user.getId(),
                stringEncryptor.encrypt(responseBody.get("access_token").toString()),
                Long.parseLong(responseBody.get("expires_in").toString()),
                TimeUnit.SECONDS);

        result.put("success", "Successfully connected Spotify account");
        return result;
    }

    public HashMap refreshAccessToken(String serverRefreshToken, User user) throws IOException, JsonProcessingException, JsonMappingException {
        HashMap result = new HashMap();

        // Build POST request to refresh access token
        final String tokenUrl = "https://accounts.spotify.com/api/token";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=refresh_token" +
                                "&refresh_token=" + URLEncoder.encode(serverRefreshToken, StandardCharsets.UTF_8) +
                                "&client_id=" + URLEncoder.encode(this.clientId, StandardCharsets.UTF_8)))
                .build();

        // Send request to spotify
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }

        // Handle response
        HashMap responseBody = new ObjectMapper().readValue(response.body(), HashMap.class);
        user.getConnections().get("spotify").put("accessToken", stringEncryptor.encrypt(responseBody.get("refresh_token").toString()));
        user.getConnections().get("spotify").put("issueDate", new Date());
        System.out.println("Refreshed Refresh Token: " + responseBody.get("refresh_token").toString());
        redisTemplate.opsForValue().set("spotifyAccessToken:" + user.getId(),
                stringEncryptor.encrypt(responseBody.get("access_token").toString()),
                Long.parseLong(responseBody.get("expires_in").toString()),
                TimeUnit.SECONDS);

        result.put("success", "Successfully refreshed Spotify access token");
        return result;
    }

    public HashMap search(String query, String type, int limit, String includeExternal, String serverAccessToken) {
        String userId = userRepository.findByUsername(serverAccessTokenService.getClaimsFromToken(serverAccessToken).getSubject())
                .map(user -> user.getId().toString()).orElseThrow(() -> new RuntimeException("User not found"));

        String spotifyAccessToken = stringEncryptor.decrypt(redisTemplate.opsForValue().get("spotifyAccessToken:" + userId));

        // Build GET request to search for tracks
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.spotify.com/v1/search" + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=" + type + "&limit=" + limit + "&include_external=" + includeExternal)).header("Authorization", "Bearer " + spotifyAccessToken).header("Accept", "application/json").GET().build();

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