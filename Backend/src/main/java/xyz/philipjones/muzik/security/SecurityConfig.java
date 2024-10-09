package xyz.philipjones.muzik.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import xyz.philipjones.muzik.repositories.UserRepository;
import xyz.philipjones.muzik.services.CustomUserDetailsService;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                        .anyRequest().authenticated() // All other requests need authentication
                )
                .oauth2Login(withDefaults()) // Enable OAuth 2.0 login
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults())); // Enable OAuth 2.0 resource server with JWT

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TODO: Check if we can cut off the custom user details service and directly insert here
    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService(userRepository);
    }

    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> myAuthenticationProviders) {
        System.out.println("Authentication Providers: " + myAuthenticationProviders.size());
        myAuthenticationProviders.forEach(provider -> System.out.println(provider.getClass().getName()));
        return new ProviderManager(myAuthenticationProviders);
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("spotify")
                .clientId("${CLIENT_ID}")
                .clientSecret("${CLIENT_SECRET}")
                .redirectUri("${REDIRECT_URI}")
                .scope("playlist-read-private", "playlist-read-collaborative")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationUri("https://accounts.spotify.com/authorize")
                .tokenUri("https://accounts.spotify.com/api/token")
                .userInfoUri("https://api.spotify.com/v1/me")
                .clientName("Spotify")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri("https://localhost:8080/.well-known/jwks.json").build();
    }
}