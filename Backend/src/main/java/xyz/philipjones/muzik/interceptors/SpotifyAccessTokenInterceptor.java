package xyz.philipjones.muzik.interceptors;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.philipjones.muzik.services.redis.RedisService;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;

@Component
public class SpotifyAccessTokenInterceptor implements HandlerInterceptor {

    private final RedisService redisService;
    private final ServerAccessTokenService serverAccessTokenService;

    public SpotifyAccessTokenInterceptor(RedisService redisService, ServerAccessTokenService serverAccessTokenService) {
        this.redisService = redisService;
        this.serverAccessTokenService = serverAccessTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path.equals("/api/v1/spotify/authorize") || path.equals("/api/v1/spotify/callback") || path.equals("/api/v1/spotify/remove-connection")
         || path.equals("/api/v1/spotify/verify")) {
            return true;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("accessToken")) {
                    String accessToken = cookie.getValue();
                    String username = serverAccessTokenService.getClaimsFromToken(accessToken).getSubject();
                    String spotifyAccessToken = redisService.getValue("spotifyAccessToken:" + username);

                    if (spotifyAccessToken == null) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write("{\"error\": \"No valid Spotify access token found\"}");
                        return false;
                    }
                }
            }
        }
        return true;
    }
}