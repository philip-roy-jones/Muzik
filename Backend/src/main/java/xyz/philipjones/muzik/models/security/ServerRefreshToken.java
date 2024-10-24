package xyz.philipjones.muzik.models.security;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "serverRefreshTokens")
public class ServerRefreshToken {

    @Id
    private ObjectId id;
    private String token;
    private String username;
    private String accessJti;
    private Date accessExpiryDate;
    private Date issuedDate;
    private Date expiryDate;
    private ObjectId userOid;

    // Getters and setters
    // DO NOT ENCRYPT/DECRYPT AT THE MODEL LEVEL
    // THIS WILL CAUSE REFLECTION ISSUES

    public ObjectId getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public ObjectId getUserOid() {
        return userOid;
    }

    public void setUserOid(ObjectId userId) {
        this.userOid = userId;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(Date issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getAccessJti() {
        return accessJti;
    }

    public void setAccessJti(String accessToken) {
        this.accessJti = accessToken;
    }

    public Date getAccessExpiryDate() {
        return accessExpiryDate;
    }

    public void setAccessExpiryDate(Date accessExpiryDate) {
        this.accessExpiryDate = accessExpiryDate;
    }
}