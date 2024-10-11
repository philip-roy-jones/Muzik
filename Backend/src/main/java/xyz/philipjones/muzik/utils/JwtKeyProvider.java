package xyz.philipjones.muzik.utils;

import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import org.springframework.stereotype.Component;

@Component
public class JwtKeyProvider {

    private final Key key;

    // This constructor generates a key if one does not exist
    public JwtKeyProvider() {
        Path keyPath = Paths.get("secret.key");
        if (!Files.exists(keyPath)) {
            try {
                JwtKeyGenerator.main(new String[]{});
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

    public Key getKey() {
        return key;
    }
}