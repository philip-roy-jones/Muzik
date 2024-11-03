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
    private final UserService userService;
    private final StringEncryptor stringEncryptor;
    private final Key key;
    private final ServerAccessTokenService serverAccessTokenService;

    @Autowired
    public ServerRefreshTokenService(ServerRefreshTokenRepository serverRefreshTokenRepository,
                                     @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                                     JwtKeyProvider jwtKeyProvider, UserService userService,
                                     ServerAccessTokenService serverAccessTokenService) {
        this.serverRefreshTokenRepository = serverRefreshTokenRepository;
        this.stringEncryptor = stringEncryptor;
        this.key = jwtKeyProvider.getKey();
        this.userService = userService;
        this.serverAccessTokenService = serverAccessTokenService;
    }

    public ServerRefreshToken generateRefreshToken(String username, boolean isRememberMe, String accessToken) {
        User user = userService.getUserByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        ServerRefreshToken refreshToken = new ServerRefreshToken();
        refreshToken.setToken(stringEncryptor.encrypt(UUID.randomUUID().toString()));
        refreshToken.setUsername(username);
        refreshToken.setAccessJti(stringEncryptor.encrypt(Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getBody().getId()));
        refreshToken.setAccessExpiryDate(Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getBody().getExpiration());
        refreshToken.setUserOid(user.getId());
        refreshToken.setIssuedDate(new Date());

        if (isRememberMe) {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + serverRefreshTokenExpirationMsRemember));
        } else {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + serverRefreshTokenExpirationMs));
        }

        serverRefreshTokenRepository.save(refreshToken);

        return refreshToken;
    }

    public ServerRefreshToken generateRefreshTokenWithExpiryDate(String username, Date expiryDate, String accessToken) {
        User user = userService.getUserByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        ServerRefreshToken refreshToken = new ServerRefreshToken();
        refreshToken.setToken(stringEncryptor.encrypt(UUID.randomUUID().toString()));
        refreshToken.setUsername(username);
        refreshToken.setAccessJti(stringEncryptor.encrypt(Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getBody().getId()));
        refreshToken.setAccessExpiryDate(Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getBody().getExpiration());
        refreshToken.setUserOid(user.getId());
        refreshToken.setIssuedDate(new Date());

        int timeLeft = (int) (expiryDate.getTime() - System.currentTimeMillis());
        if (timeLeft > 0) {
            refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + timeLeft));
        } else {
            return null;
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
    }

    public String getRefreshToken(ServerRefreshToken refreshToken) {
        return stringEncryptor.decrypt(refreshToken.getToken());
    }

    public String encryptRefreshToken(String refreshToken) {
        return stringEncryptor.encrypt(refreshToken);
    }

    public ServerRefreshToken findByToken(String refreshToken) {
        return serverRefreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    public ServerRefreshToken findByUsername(String username) {
        return serverRefreshTokenRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    public boolean saveRefreshToken(ServerRefreshToken refreshToken) {
        try {
            serverRefreshTokenRepository.save(refreshToken);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteRefreshToken(ServerRefreshToken refreshToken) {
        serverRefreshTokenRepository.delete(refreshToken);
    }

    public void setAccessJti(ServerRefreshToken refreshToken, String accessToken) {
        refreshToken.setAccessJti(serverAccessTokenService.encryptJti(serverAccessTokenService.getClaimsFromToken(accessToken)));
    }

    public void setAccessExpiryDate(ServerRefreshToken refreshToken) {
        refreshToken.setAccessExpiryDate(new Date(System.currentTimeMillis() + serverAccessTokenService.getAccessTokenExpirationInMs()));
    }
}