package xyz.philipjones.muzik.services.security;

import io.jsonwebtoken.Jwts;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.ServerRefreshToken;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.repositories.ServerRefreshTokenRepository;
import xyz.philipjones.muzik.repositories.UserRepository;
import xyz.philipjones.muzik.utils.JwtKeyProvider;

import java.security.Key;
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
    private final Key key;

    @Autowired
    public ServerRefreshTokenService(ServerRefreshTokenRepository serverRefreshTokenRepository,
                                     UserRepository userRepository,
                                     @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                                     JwtKeyProvider jwtKeyProvider) {
        this.serverRefreshTokenRepository = serverRefreshTokenRepository;
        this.userRepository = userRepository;
        this.stringEncryptor = stringEncryptor;
        this.key = jwtKeyProvider.getKey();
    }

    public ServerRefreshToken generateRefreshToken(String username, boolean isRememberMe, String accessToken) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        ServerRefreshToken refreshToken = new ServerRefreshToken();
        refreshToken.setToken(stringEncryptor.encrypt(UUID.randomUUID().toString()));
        refreshToken.setUsername(username);
        refreshToken.setAccessJti(stringEncryptor.encrypt(Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getBody().getId()));
        refreshToken.setUserId(user.getId());
        refreshToken.setIssuedDate(new Date());

        if (isRememberMe) {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + serverRefreshTokenExpirationMsRemember));
        } else {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + serverRefreshTokenExpirationMs));
        }

        serverRefreshTokenRepository.save(refreshToken);

        return refreshToken;
    }

    // True if the token is valid, false otherwise
    public boolean validateRefreshToken(String token) {
        Optional<ServerRefreshToken> refreshTokenOpt = serverRefreshTokenRepository.findByToken(stringEncryptor.encrypt(token));

        if (refreshTokenOpt.isPresent()) {
            ServerRefreshToken refreshToken = refreshTokenOpt.get();
            return refreshToken.getExpiryDate().after(new Date()) && refreshToken.getIssuedDate().before(new Date());
        }
        return false;
        // TODO: We need to see what happens when the token is expired or not found
    }
}