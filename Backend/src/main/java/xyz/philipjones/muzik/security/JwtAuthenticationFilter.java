package xyz.philipjones.muzik.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;

import java.io.IOException;
import java.util.Arrays;

// This class is responsible for filtering incoming requests and checking for a valid JWT token.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ServerAccessTokenService serverAccessTokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(ServerAccessTokenService serverAccessTokenService) {
        this.serverAccessTokenService = serverAccessTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean shouldSkip = pathMatcher.match("/public/**", request.getServletPath());
        return shouldSkip;
    }

    // FilterChain is used to pass the request along the chain of filters, like using multiple filters in Excel
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getJwtFromCookies(request);
        System.out.println("Token in doFilterInternal: " + token);

        if (token != null && serverAccessTokenService.validateAccessToken(token)) {
            Claims claims = serverAccessTokenService.getClaimsFromToken(token);
            String username = claims.getSubject();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}