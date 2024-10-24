package xyz.philipjones.muzik.models.spotify;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document(collection = "tracks")
public class Track {

    @Id
    private ObjectId id;
    private String spotifyId;
    private String name;
    private boolean explicit;
    @Indexed
    private ArrayList<ObjectId> artistOids = new ArrayList<>();     // First artist is the main artist
    @Indexed
    private ObjectId albumOid;

    // Getters and setters

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

    public boolean isExplicit() {
        return explicit;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public ArrayList<ObjectId> getArtistOids() {
        return artistOids;
    }

    public void setArtistOids(ArrayList<ObjectId> artistOids) {
        this.artistOids = artistOids;
    }

    public ObjectId getAlbumOid() {
        return albumOid;
    }

    public void setAlbumOid(ObjectId albumOid) {
        this.albumOid = albumOid;
    }
}
