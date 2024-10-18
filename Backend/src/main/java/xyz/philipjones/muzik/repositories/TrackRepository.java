package xyz.philipjones.muzik.repositories;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.philipjones.muzik.models.spotify.Track;

public interface TrackRepository extends MongoRepository<Track, ObjectId> {
}
