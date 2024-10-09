package xyz.philipjones.muzik.services.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.ServerRefreshToken;
import xyz.philipjones.muzik.repositories.ServerRefreshTokenRepository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${refresh.token.expiration}")
    private long serverRefreshTokenExpirationMs;

    private final ServerRefreshTokenRepository serverRefreshTokenRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createRefreshToken(Authentication authentication) {
        String username = authentication.getName();
        String token = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUsername(username);
        refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshTokenExpirationMs));

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean validateRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);
        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            return refreshToken.getExpiryDate().after(new Date());
        }
        return false;
    }

    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}