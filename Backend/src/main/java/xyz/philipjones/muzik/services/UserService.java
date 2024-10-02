package xyz.philipjones.muzik.services;

import xyz.philipjones.muzik.models.User;

import java.util.Date;

public interface UserService {
    User getUser(String spotifyUserId);
    void updateTokens(User user, String newAccessToken, String newRefreshToken, Date newExpiresAt);
    boolean isAccessTokenExpired(User user);
}