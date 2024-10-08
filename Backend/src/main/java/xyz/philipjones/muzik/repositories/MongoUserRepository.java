package xyz.philipjones.muzik.repositories;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.MongoUser;

public interface MongoUserRepository extends MongoRepository<MongoUser, String> {
}