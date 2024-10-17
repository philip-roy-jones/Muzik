package xyz.philipjones.muzik.interceptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.services.RedisService;
import xyz.philipjones.muzik.services.SpotifyTokenService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.philipjones.muzik.services.security.UserService;

// Interceptor to check if the user has a valid Spotify access token before making requests to the Spotify API
@Component
public class SpotifyTokenInterceptor implements HandlerInterceptor {

    private final ServerAccessTokenService serverAccessTokenService;
    private final RedisService redisService;
    private final UserService userService;
    private final SpotifyTokenService spotifyTokenService;

    @Autowired
    public SpotifyTokenInterceptor(ServerAccessTokenService serverAccessTokenService, RedisService redisService,
                                   UserService userService, SpotifyTokenService spotifyTokenService) {
        this.serverAccessTokenService = serverAccessTokenService;
        this.redisService = redisService;
        this.userService = userService;
        this.spotifyTokenService = spotifyTokenService;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            String username = serverAccessTokenService.getClaimsFromToken(token).getSubject();
            User user = userService.getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String spotifyRefreshToken = userService.getSpotifyRefreshToken(user);
            String userId = user.getId().toString();
            String redisKey = "spotifyAccessToken:" + userId;

            if (redisService.hasKey(redisKey)) {
                return true; // Token exists in Redis, proceed with the request
            } else if (spotifyRefreshToken != null) {
                spotifyTokenService.refreshAccessToken(spotifyRefreshToken, user);
                return true;
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("User needs to authorize Spotify");
                return false; // User needs to authorize Spotify, block the request
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Authorization header missing or invalid");
        return false; // Authorization header missing or invalid, block the request
    }
}