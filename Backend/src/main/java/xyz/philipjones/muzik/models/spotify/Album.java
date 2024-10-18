package xyz.philipjones.muzik.models.spotify;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document(collection = "albums")
public class Album {

    @Id
    private ObjectId id;
    private String spotifyId;
    private String type;
    private String name;
    private String releaseDate;
    private String releaseDatePrecision;
    private Integer totalTracks;
    @Indexed
    private ArrayList<ObjectId> artistOids;
    @Indexed
    private ArrayList<ObjectId> trackOids;

    // Getters and setters

    public ObjectId getId() {
        return id;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getReleaseDatePrecision() {
        return releaseDatePrecision;
    }

    public void setReleaseDatePrecision(String releaseDatePrecision) {
        this.releaseDatePrecision = releaseDatePrecision;
    }

    public Integer getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(Integer totalTracks) {
        this.totalTracks = totalTracks;
    }

    public ArrayList<ObjectId> getArtistOids() {
        return artistOids;
    }

    public void setArtistOids(ArrayList<ObjectId> artistOids) {
        this.artistOids = artistOids;
    }

    public ArrayList<ObjectId> getTrackOids() {
        return trackOids;
    }

    public void setTrackOids(ArrayList<ObjectId> trackOids) {
        this.trackOids = trackOids;
    }
}
