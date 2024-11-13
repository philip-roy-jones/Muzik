package xyz.philipjones.muzik.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.philipjones.muzik.interceptors.SpotifyAccessTokenInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SpotifyAccessTokenInterceptor spotifyAccessTokenInterceptor;

    @Autowired
    public WebConfig(SpotifyAccessTokenInterceptor spotifyAccessTokenInterceptor) {
        this.spotifyAccessTokenInterceptor = spotifyAccessTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(spotifyAccessTokenInterceptor).addPathPatterns("/api/v1/spotify/**")
                .excludePathPatterns("/api/v1/spotify/authorize", "/api/v1/spotify/callback", "/api/v1/spotify/remove-connection");
    }
}