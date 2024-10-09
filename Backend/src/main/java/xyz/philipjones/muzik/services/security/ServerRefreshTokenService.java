package xyz.philipjones.muzik.services.security;

import org.bson.types.ObjectId;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.ServerRefreshToken;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.repositories.ServerRefreshTokenRepository;
import xyz.philipjones.muzik.repositories.UserRepository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServerRefreshTokenService {

    @Value("${refresh.token.expiration.remember}")
    private long serverRefreshTokenExpirationMsRemember;

    @Value("${refresh.token.expiration}")
    private long serverRefreshTokenExpirationMs;

    private final ServerRefreshTokenRepository serverRefreshTokenRepository;
    private final UserRepository userRepository;
    private final StringEncryptor stringEncryptor;

    @Autowired                                                                                                                  // I have no clue why there are two StringEncryptor beans
    public ServerRefreshTokenService(ServerRefreshTokenRepository serverRefreshTokenRepository, UserRepository userRepository, @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.serverRefreshTokenRepository = serverRefreshTokenRepository;
        this.userRepository = userRepository;
        this.stringEncryptor = stringEncryptor;
    }

    public String createRefreshToken(Authentication authentication, boolean isRememberMe) {
        String username = authentication.getName();
        String token = UUID.randomUUID().toString();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        ServerRefreshToken refreshToken = new ServerRefreshToken();
        refreshToken.setToken(stringEncryptor.encrypt(token));
        refreshToken.setUsername(username);
        refreshToken.setUserId(user.getId());
        refreshToken.setIssuedDate(new Date());

        if (isRememberMe) {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + serverRefreshTokenExpirationMsRemember));
        } else {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + serverRefreshTokenExpirationMs));
        }

        serverRefreshTokenRepository.save(refreshToken);

        return token;
    }

    public Optional<ServerRefreshToken> findByToken(String token) {
        return serverRefreshTokenRepository.findByToken(token);
    }

    public boolean validateRefreshToken(String token) {
        Optional<ServerRefreshToken> refreshTokenOpt = serverRefreshTokenRepository.findByToken(token);
        if (refreshTokenOpt.isPresent()) {
            ServerRefreshToken refreshToken = refreshTokenOpt.get();
            return refreshToken.getExpiryDate().after(new Date());
        }
        return false;
    }

    public void deleteRefreshToken(String token) {
        serverRefreshTokenRepository.deleteByToken(token);
    }
}