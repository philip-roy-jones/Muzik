package xyz.philipjones.muzik.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.spotify.Artist;

public interface ArtistRepository extends MongoRepository<Artist, ObjectId> {
}
