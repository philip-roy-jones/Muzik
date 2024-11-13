package xyz.philipjones.muzik.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.security.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, ObjectId> {
    Optional<User> findByUsername(String username);
}