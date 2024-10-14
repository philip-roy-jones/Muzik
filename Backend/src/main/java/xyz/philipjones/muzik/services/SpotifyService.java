package xyz.philipjones.muzik.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.repositories.UserRepository;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class SpotifyService {

    private final UserRepository userRepository;
    private final ServerAccessTokenService serverAccessTokenService;
    private final StringEncryptor stringEncryptor;
    private final RedisTemplate<String, String> redisTemplate;

    public SpotifyService(UserRepository userRepository, ServerAccessTokenService serverAccessTokenService,
                          @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                          RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.serverAccessTokenService = serverAccessTokenService;
        this.stringEncryptor = stringEncryptor;
        this.redisTemplate = redisTemplate;
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
