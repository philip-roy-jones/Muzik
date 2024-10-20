package xyz.philipjones.muzik.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="randomStringErrors")
public class RandomStringError {

    @Id
    private ObjectId id;
    private String randomString;

    public ObjectId getId() {
        return id;
    }

    public String getRandomString() {
        return randomString;
    }

    public void setRandomString(String randomString) {
        this.randomString = randomString;
    }
}
