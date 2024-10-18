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
    private HashSet<ObjectId> albumOids;
}
