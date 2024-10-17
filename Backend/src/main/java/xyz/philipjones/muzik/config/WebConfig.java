package xyz.philipjones.muzik.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.philipjones.muzik.interceptors.SpotifyTokenInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SpotifyTokenInterceptor spotifyTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(spotifyTokenInterceptor)
                .addPathPatterns("/api/v1/spotify/**")
                .excludePathPatterns(
                        "/api/v1/spotify/authorize",
                        "/api/v1/spotify/callback",
                        "/api/v1/spotify/remove-connection"
                );
    }
}