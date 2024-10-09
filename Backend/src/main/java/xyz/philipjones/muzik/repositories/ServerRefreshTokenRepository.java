package xyz.philipjones.muzik.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.security.ServerRefreshToken;

import java.util.Optional;

public interface ServerRefreshTokenRepository extends MongoRepository<ServerRefreshToken, Long> {
    Optional<ServerRefreshToken> findByToken(String token);
    void deleteByToken(String token);
}