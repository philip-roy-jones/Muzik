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
import xyz.philipjones.muzik.models.security.ServerRefreshToken;
import xyz.philipjones.muzik.repositories.ServerRefreshTokenRepository;
import xyz.philipjones.muzik.utils.JwtKeyProvider;

import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ServerAccessTokenService {

    @Value("${access.token.expiration}")
    private long accessTokenExpirationInMs;

    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    private final Key key;
    private final ServerRefreshTokenRepository serverRefreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringEncryptor stringEncryptor;
    private final ServerRefreshTokenService serverRefreshTokenService;

    @Autowired
    public ServerAccessTokenService(RedisTemplate<String, Object> redisTemplate, ServerRefreshTokenRepository serverRefreshTokenRepository,
                                    @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,            // I have no clue why there are two StringEncryptor beans
                                    ServerRefreshTokenService serverRefreshTokenService,
                                    JwtKeyProvider jwtKeyProvider) {

        this.redisTemplate = redisTemplate;
        this.serverRefreshTokenRepository = serverRefreshTokenRepository;
        this.stringEncryptor = stringEncryptor;
        this.serverRefreshTokenService = serverRefreshTokenService;
        this.key = jwtKeyProvider.getKey();
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
            if (Boolean.TRUE.equals(redisTemplate.hasKey("accessToken:blacklist:" + claims.getId()))) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseSignedClaims(token)
                .getBody();
    }

    public void blacklistAccessToken(String Jti) {
        // TODO: This could calculate the time until expiration and blacklist it for that long to reduce the amount of memory used
        redisTemplate.opsForValue().set("accessToken:blacklist:" + Jti, "blacklisted", accessTokenExpirationInMs, TimeUnit.MILLISECONDS);
    }

}