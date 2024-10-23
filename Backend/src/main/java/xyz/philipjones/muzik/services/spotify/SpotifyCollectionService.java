package xyz.philipjones.muzik.services.spotify;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.spotify.Album;
import xyz.philipjones.muzik.models.spotify.Artist;
import xyz.philipjones.muzik.models.spotify.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Service
public class SpotifyCollectionService {

    private final MongoTemplate mongoTemplate;
    private final MongoClient mongoClient;

    @Autowired
    public SpotifyCollectionService(MongoTemplate mongoTemplate, MongoClient mongoClient) {
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
    }

    @Async
    public void createAndSaveTrackWithAlbumAndArtists(HashMap data) {
        // Create Track instance
        Track track = new Track();
        track.setId(new ObjectId());
        track.setSpotifyId(data.get("id").toString());
        track.setName(data.get("name").toString());
        track.setExplicit(data.get("explicit").toString().equals("true"));

        // Create Album instance
        HashMap albumData = (HashMap) data.get("album");
        Album album = new Album();
        album.setId(new ObjectId());
        album.setSpotifyId(albumData.get("id").toString());
        album.setType(albumData.get("album_type").toString());
        album.setName(albumData.get("name").toString());
        album.setReleaseDate(albumData.get("release_date").toString());
        album.setReleaseDatePrecision(albumData.get("release_date_precision").toString());
        album.setTotalTracks(Integer.parseInt(albumData.get("total_tracks").toString()));
        album.setImages((ArrayList) albumData.get("images"));

        // Create List of Artist instances
        List<HashMap> artistsData = (List<HashMap>) data.get("artists");

        List<Artist> artists = new ArrayList<>();
        for (HashMap artistData : artistsData) {
            Artist artist = new Artist();
            artist.setId(new ObjectId());
            artist.setSpotifyId(artistData.get("id").toString());
            artist.setName(artistData.get("name").toString());

            artists.add(artist);
        }

        saveTrackWithAlbumAndArtists(track, album, artists);
    }

    public void saveTrackWithAlbumAndArtists(Track trackData, Album albumData, List<Artist> artistDataList) {
        // Check if track already exists
        Query trackQuery = new Query(Criteria.where("spotifyId").is(trackData.getSpotifyId()));
        Track existingTrack = mongoTemplate.findOne(trackQuery, Track.class);
        if (existingTrack != null) {
            return;
        }

        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();

            // Save or update artists
            ArrayList<ObjectId> savedArtists = new ArrayList<>();
            for (Artist artistData : artistDataList) {
                Query query = new Query(Criteria.where("spotifyId").is(artistData.getSpotifyId()));
                Artist existingArtist = mongoTemplate.findOne(query, Artist.class);
                if (existingArtist == null) {
                    mongoTemplate.save(artistData);
                    savedArtists.add(artistData.getId());
                } else {
                    // Optionally, update existing artist if needed
                    existingArtist.getAlbumOids().add(albumData.getId());
                    existingArtist.setAlbumOids(existingArtist.getAlbumOids());
                    mongoTemplate.save(existingArtist);
                    savedArtists.add(existingArtist.getId());
                }
            }

            // Save or update album
            Query albumQuery = new Query(Criteria.where("spotifyId").is(albumData.getSpotifyId()));
            Album existingAlbum = mongoTemplate.findOne(albumQuery, Album.class);
            if (existingAlbum == null) {
                // TODO: savedArtists is tied to a track, we need a separate album artists.
                //  This is because a track can have multiple artists, but an album could have a single artist.
                albumData.setArtistOids(savedArtists); // Reference saved artists

                // Set album's track oids
                // This does not order the tracks in the album, no plans to show user the tracks in the album
                albumData.getTrackOids().add(trackData.getId());
                albumData.setTrackOids(albumData.getTrackOids());
                mongoTemplate.save(albumData);
            } else {
                // Optionally, update existing album
                existingAlbum.setArtistOids(savedArtists); // Update artists

                existingAlbum.getTrackOids().add(trackData.getId()); // Update tracks
                existingAlbum.setTrackOids(existingAlbum.getTrackOids());
                mongoTemplate.save(existingAlbum);
            }

            // Save track, linking to the album and artists
            trackData.setAlbumOid(albumData.getId());
            trackData.setArtistOids(savedArtists);
            mongoTemplate.save(trackData);

            session.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }
}
