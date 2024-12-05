package xyz.philipjones.muzik.services.spotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.undercouch.bson4jackson.BsonModule;
import org.bson.types.ObjectId;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.config.ObjectIdDeserializer;
import xyz.philipjones.muzik.services.redis.RedisService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;
import xyz.philipjones.muzik.services.security.UserService;
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

@Service
public class SpotifyTokenService {

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.redirect-uri}")
    private String redirectUri;

    private final String codeVerifier = PKCEUtil.generateCodeVerifier(128);
    private final String state = new BigInteger(130, new SecureRandom()).toString(32);

    private final RedisService redisService;
    private final ServerAccessTokenService serverAccessTokenService;
    private final UserService userService;
    private final SpotifyHarvestService spotifyHarvestService;
    private final StringEncryptor stringEncryptor;

    @Autowired
    public SpotifyTokenService(RedisService redisService, ServerAccessTokenService serverAccessTokenService,
                               UserService userService,
                               @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                               SpotifyHarvestService spotifyHarvestService) {
        this.redisService = redisService;
        this.serverAccessTokenService = serverAccessTokenService;
        this.userService = userService;
        this.stringEncryptor = stringEncryptor;
        this.spotifyHarvestService = spotifyHarvestService;
    }

    public String getAuthorizationUrl(String serverAccessToken) throws NoSuchAlgorithmException {
        User user = userService.getUserByUsername(serverAccessTokenService.getClaimsFromToken(serverAccessToken).getSubject())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId().toString();

        String scope = "user-read-private user-read-email playlist-read-collaborative";
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);

        redisService.setValueWithExpiration("spotifyState:" + this.state, userId, 600000);

        return "https://accounts.spotify.com/authorize" + "?response_type=code" + "&client_id=" + this.clientId + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) + "&redirect_uri=" + URLEncoder.encode(this.redirectUri, StandardCharsets.UTF_8) + "&state=" + this.state + "&code_challenge=" + codeChallenge + "&code_challenge_method=S256";
    }

    public HashMap handleSpotifyCallback(String code, String receivedState) throws IOException {
        HashMap<String, String> result = new HashMap<>();

        if (!receivedState.equals(this.state)) {
            result.put("error", "State mismatch");
        }

        String storedStateStr = redisService.getValue("spotifyState:" + receivedState); // Get the user ID from the state
        ObjectId storedState = storedStateStr != null ? new ObjectId(storedStateStr) : null; // Convert the user ID to an ObjectId
        if (storedState == null) {
            result.put("error", "State not found");
            return result;
        }

        redisService.deleteKey("spotifyState:" + receivedState); // Delete the state from Redis since we have found it

        User user = userService.getUserById(storedState).orElse(null);
        if (user == null) {
            result.put("error", "User not found");
            return result;
        }

        HashMap<String, Object> jsonResponse = tokenApiCall("authorization_code", code, "", codeVerifier, user);
        tokenApiResponseHandler(jsonResponse);

        result.put("success", "Successfully connected Spotify account");
        return result;
    }

    public boolean refreshAccessToken(String spotifyRefreshToken, User user) throws IOException {
        try {
            HashMap<String, Object> jsonResponse = tokenApiCall("refresh_token", "", spotifyRefreshToken, "", user);
            tokenApiResponseHandler(jsonResponse);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public void removeConnection(String serverAccessToken) {
        User user = userService.getUserByUsername(serverAccessTokenService.getClaimsFromToken(serverAccessToken).getSubject()).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        userService.removeConnection(user, "spotify");          // Remove connection from MongoDB
        redisService.deleteKey("spotifyAccessToken:" + user.getUsername());   // Remove spotify access token from Redis
    }

    public String getSpotifyAccessToken(String username) {
        User user = userService.getUserByUsername(username).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        String encryptedAccessToken = redisService.getValue("spotifyAccessToken:" + user.getUsername());
        if (encryptedAccessToken == null) {
            return null;
        }

        return stringEncryptor.decrypt(encryptedAccessToken);
    }

    public long getSpotifyAccessTokenExpiration(String username) {
        return redisService.getExpiration("spotifyAccessToken:" + username);
    }

    public void deleteSpotifyAccessToken(String username) {
        redisService.deleteKey("spotifyAccessToken:" + username);
    }

    public boolean verifyConnection(String accessToken) {
        String username = serverAccessTokenService.getClaimsFromToken(accessToken).getSubject();
        User user = userService.getUserByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        return user.getConnections().containsKey("spotify");
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
//        System.out.println("Spotify Access token: " + accessToken);
        if (grantType.equals("refresh_token") && refreshToken != null) {    // Spotify sometimes provides new refresh tokens
            user.getConnections().get("spotify").put("refreshToken", stringEncryptor.encrypt(refreshToken));
            user.getConnections().get("spotify").put("issueDate", new Date());
        } else if (grantType.equals("authorization_code")) {
            HashMap<String, Object> connection = new HashMap<>();
            connection.put("refreshToken", stringEncryptor.encrypt(refreshToken));
            connection.put("issueDate", new Date());
            user.addConnection("spotify", connection);
        } else {
            throw new IllegalArgumentException("Invalid grant type");
        }
        userService.saveUser(user);

        redisService.setValueWithExpiration("spotifyAccessToken:" + user.getUsername(),
                stringEncryptor.encrypt(accessToken), expiresIn * 1000);

        // Initialize or re-init Spotify queues
        spotifyHarvestService.initSpotifyQueues(user.getUsername());
    }
}