package xyz.philipjones.muzik.models;

import java.io.Serializable;

public class RedisUser implements Serializable {
    private String id;
    private String accessToken;
    private long expiresAt;

    public RedisUser() {}

    public RedisUser(String id, String accessToken, long expiresAt) {
        this.id = id;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}