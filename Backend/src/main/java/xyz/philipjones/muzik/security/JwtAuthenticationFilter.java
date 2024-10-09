package xyz.philipjones.muzik.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.philipjones.muzik.services.security.ServerAccessTokenService;

import java.io.IOException;

// This class is responsible for filtering incoming requests and checking for a valid JWT token.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ServerAccessTokenService serverAccessTokenService;

    public JwtAuthenticationFilter(ServerAccessTokenService serverAccessTokenService) {
        this.serverAccessTokenService = serverAccessTokenService;
    }

    // FilterChain is used to pass the request along the chain of filters, like using multiple filters in Excel
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getJwtFromRequest(request);

        if (token != null && serverAccessTokenService.validateToken(token)) {
            Claims claims = serverAccessTokenService.getClaimsFromToken(token);
            String username = claims.getSubject();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, null);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {

            // The substring(7) method is used to remove the "Bearer " prefix from the token
            return bearerToken.substring(7);
        }
        return null;
    }
}
