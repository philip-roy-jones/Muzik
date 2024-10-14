package xyz.philipjones.muzik.services.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.utils.JwtKeyProvider;

import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ServerAccessTokenService {

    @Value("${access.token.expiration}")
    private long accessTokenExpirationInMs;

    @Value("${server.address}")             // TODO: See if we can remove this variable, don't want to have to go back and add this separately during deployment
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    private final Key key;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringEncryptor stringEncryptor;

    @Autowired
    public ServerAccessTokenService(RedisTemplate<String, Object> redisTemplate, JwtKeyProvider jwtKeyProvider,
                                    @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {

        this.redisTemplate = redisTemplate;
        this.key = jwtKeyProvider.getKey();
        this.stringEncryptor = stringEncryptor;
    }

    public String generateAccessToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://" + serverAddress + ":" + serverPort);
        claims.put("sub", username);
        claims.put("aud", "https://" + serverAddress + ":" + serverPort);          // TODO: We should tie it to a subdomain
        claims.put("exp", new Date(System.currentTimeMillis() + accessTokenExpirationInMs));
        claims.put("nbf", new Date(System.currentTimeMillis()));
        claims.put("iat", new Date(System.currentTimeMillis()));
        claims.put("jti", java.util.UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            // Validating signature
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Validating claims
            String username = claims.getSubject();
            if (username == null || username.isEmpty()) {
                return false;
            }

            // Validating expiration
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                return false; // Token is expired
            }

            // Validating blacklist
            if (Boolean.TRUE.equals(redisTemplate.hasKey("serverAccessToken:blacklist:" + claims.getId()))) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String accessToken) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseSignedClaims(accessToken)
                .getBody();
    }

    public void blacklistAccessToken(String encryptedJti) {
        // Already encrypted jti are passed through to here, all we need to do is add to Redis
        // TODO: This could calculate the time until expiration and blacklist it for that long to reduce the amount of memory used
        redisTemplate.opsForValue().set("serverAccessToken:blacklist:" + encryptedJti, "blacklisted", accessTokenExpirationInMs, TimeUnit.MILLISECONDS);
    }

    public String encryptJti(Claims claims) {
        return stringEncryptor.encrypt(claims.getId());
    }
}