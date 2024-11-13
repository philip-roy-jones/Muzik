package xyz.philipjones.muzik.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.security.UserRole;

import java.util.Optional;

public interface UserRolesRepository extends MongoRepository<UserRole, String> {
   Optional<UserRole> findByName(String roleUser);
}