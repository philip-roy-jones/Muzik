package xyz.philipjones.muzik.models.spotify;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashSet;

@Document(collection = "artists")
public class Artist {

    @Id
    private ObjectId id;
    private String spotifyId;
    private String name;
    private HashSet<ObjectId> albumOids = new HashSet<>();

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashSet<ObjectId> getAlbumOids() {
        return albumOids;
    }

    public void setAlbumOids(HashSet<ObjectId> albumOids) {
        this.albumOids = albumOids;
    }
}
