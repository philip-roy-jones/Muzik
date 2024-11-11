package xyz.philipjones.muzik.services.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import xyz.philipjones.muzik.services.redis.RedisService;
import xyz.philipjones.muzik.utils.JwtKeyProvider;

import java.security.Key;
import java.util.*;

@Service
public class ServerAccessTokenService {

    @Value("${access.token.expiration}")
    private long accessTokenExpirationInMs;

    private final Key key;
    private final RedisService redisService;
    private final StringEncryptor stringEncryptor;

    @Autowired
    public ServerAccessTokenService(RedisService redisService, JwtKeyProvider jwtKeyProvider,
                                    @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {

        this.redisService = redisService;
        this.key = jwtKeyProvider.getKey();
        this.stringEncryptor = stringEncryptor;
    }

    public String generateAccessToken(String username) {
        // TODO: Add the user's roles to the claims, maybe we should pass in User object instead

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());  // Sets the issuer (iss) claim to the current server's URI
        claims.put("sub", username);                                                                    // Cont.: this avoids hardcoding the server URI
        claims.put("aud", ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
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

            // TODO: Change to if the access token is in Redis = valid
            // Validating blacklist
            if (Boolean.TRUE.equals(redisService.hasKey("serverAccessToken:blacklist:" + stringEncryptor.encrypt(claims.getId())))) {
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

    public void blacklistAccessToken(String encryptedJti, Date expiration) {
        // An ENCRYPTED jti is passed through, all we need to do is add to Redis
        redisService.setValueWithExpiration("serverAccessToken:blacklist:" + encryptedJti, "blacklisted", expiration.getTime() - System.currentTimeMillis());
    }

    public String encryptJti(Claims claims) {
        return stringEncryptor.encrypt(claims.getId());
    }

    public long getAccessTokenExpirationInMs() {
        return accessTokenExpirationInMs;
    }
}