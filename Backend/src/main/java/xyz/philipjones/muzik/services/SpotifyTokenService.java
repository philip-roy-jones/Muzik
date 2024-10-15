package xyz.philipjones.muzik.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.undercouch.bson4jackson.BsonModule;
import org.bson.types.ObjectId;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.config.ObjectIdDeserializer;
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
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SpotifyTokenService {

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
    public SpotifyTokenService(RedisTemplate<String, String> redisTemplate, ServerAccessTokenService serverAccessTokenService,
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

        HashMap<String, Object> jsonResponse = tokenApiCall("authorization_code", code, "", codeVerifier, user);
        tokenApiResponseHandler(jsonResponse);

        result.put("success", "Successfully connected Spotify account");
        return result;
    }

    public HashMap refreshAccessToken(String serverRefreshToken, User user) throws IOException {
        HashMap result = new HashMap();

        HashMap<String, Object> jsonResponse = tokenApiCall("refresh_token", "", serverRefreshToken, "", user);
        tokenApiResponseHandler(jsonResponse);

        result.put("success", "Successfully refreshed Spotify access token");
        return result;
    }

    //---------------------------------------------Privates---------------------------------------------

    private HashMap<String, Object> tokenApiCall(String grantType, String code, String refreshToken, String codeVerifier, User user) throws IOException {
        final String tokenUrl = "https://accounts.spotify.com/api/token";

        // Build POST request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded");

        StringBuilder requestBody = new StringBuilder("grant_type=" + URLEncoder.encode(grantType, StandardCharsets.UTF_8));
        if (grantType.equals("authorization_code")) {
            requestBody.append("&code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8))
                    .append("&redirect_uri=").append(URLEncoder.encode(this.redirectUri, StandardCharsets.UTF_8))
                    .append("&client_id=").append(URLEncoder.encode(this.clientId, StandardCharsets.UTF_8))
                    .append("&code_verifier=").append(URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8));
        } else if (grantType.equals("refresh_token")) {
            requestBody.append("&refresh_token=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    .append("&client_id=").append(URLEncoder.encode(this.clientId, StandardCharsets.UTF_8));
        }

        HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody.toString())).build();

        // Send request to Spotify
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            HashMap<String, Object> responseBody = new ObjectMapper().readValue(response.body(), HashMap.class);
            responseBody.put("grant_type", grantType);
            responseBody.put("user", user);
            return responseBody;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new IOException("Request interrupted", e);
        }
    }

    private void tokenApiResponseHandler(HashMap<String, Object> responseMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        // Register BsonModule and this project's serializer/deserializer to handle ObjectId in the User class
        objectMapper.registerModule(new BsonModule());
        objectMapper.registerModule(new SimpleModule().addDeserializer(ObjectId.class, new ObjectIdDeserializer())
                .addDeserializer(ObjectId.class, new ObjectIdDeserializer()));

        String grantType = (String) responseMap.get("grant_type");
        String refreshToken = (String) responseMap.get("refresh_token");
        String accessToken = (String) responseMap.get("access_token");
        Integer expiresIn = (Integer) responseMap.get("expires_in");
        User user = objectMapper.convertValue(responseMap.get("user"), User.class);

        if (grantType.equals("refresh_token")) {
            user.getConnections().get("spotify").put("accessToken", stringEncryptor.encrypt(refreshToken));
            user.getConnections().get("spotify").put("issueDate", new Date());
        } else if (grantType.equals("authorization_code")) {
            HashMap<String, Object> connection = new HashMap<>();
            connection.put("refreshToken", stringEncryptor.encrypt(refreshToken));
            connection.put("issueDate", new Date());
            user.addConnection("spotify", connection);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Invalid grant type");
        }

        redisTemplate.opsForValue().set("spotifyAccessToken:" + user.getId(),
                stringEncryptor.encrypt(accessToken),
                expiresIn,
                TimeUnit.SECONDS);
    }
}