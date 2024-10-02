package xyz.philipjones.muzik.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.User;
import xyz.philipjones.muzik.repositories.UserRepository;

import java.util.Date;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public User getUser(String spotifyUserId) {
        return userRepository.findBySpotifyUserId(spotifyUserId);
    }

    @Override
    public void updateTokens(User user, String newAccessToken, String newRefreshToken, Date newExpiresAt) {
        user.setAccessToken(newAccessToken);
        user.setRefreshToken(newRefreshToken);
        user.setExpiresAt(newExpiresAt);
        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    @Override
    public boolean isAccessTokenExpired(User user) {
        return new Date().after(user.getExpiresAt());
    }
}