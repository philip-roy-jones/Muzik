package xyz.philipjones.muzik.services.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.utils.KeyGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ServerAccessTokenService {
    private final Key key;

    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    public ServerAccessTokenService() {

        // Using a persisted key to allow use after server restart
        Path keyPath = Paths.get("secret.key");
        if (!Files.exists(keyPath)) {
            try {
                KeyGenerator.main(new String[]{});
            } catch (IOException e) {
                throw new RuntimeException("Failed to generate key", e);
            }
        }

        try {
            byte[] keyBytes = Files.readAllBytes(keyPath);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read key from file", e);
        }
    }

    public String generateToken(Authentication authentication) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://" + serverAddress + ":" + serverPort);
        claims.put("sub", authentication.getName());
        claims.put("aud", "https://" + serverAddress + ":" + serverPort);          // TODO: We should tie it to a subdomain
        claims.put("exp", new Date(System.currentTimeMillis() + jwtExpirationInMs));
        claims.put("nbf", new Date(System.currentTimeMillis()));
        claims.put("iat", new Date(System.currentTimeMillis()));
        claims.put("jti", java.util.UUID.randomUUID().toString());                              // TODO: need to store JTI to allow for revocation

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(authentication.getName()) // Set the subject as the username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }

    public boolean validateToken(String token) {
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
}