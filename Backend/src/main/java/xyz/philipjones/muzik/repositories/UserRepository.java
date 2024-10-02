package xyz.philipjones.muzik.repositories;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.User;

public interface UserRepository extends MongoRepository<User, String> {
    User findBySpotifyUserId(String spotifyUserId);
}