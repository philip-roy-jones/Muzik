package xyz.philipjones.muzik.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.security.Role;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByUsername(String username);
}