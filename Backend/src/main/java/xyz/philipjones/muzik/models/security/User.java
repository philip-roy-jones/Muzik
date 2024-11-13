package xyz.philipjones.muzik.models.security;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import xyz.philipjones.muzik.config.ObjectIdDeserializer;
import xyz.philipjones.muzik.config.ObjectIdSerializer;
import xyz.philipjones.muzik.repositories.UserRolesRepository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Document(collection = "users")
public class User {
    @Id
    @JsonSerialize(using = ObjectIdSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    @Indexed(unique = true)
    private String username;
    private String email;
    private String password;  // Stores hashed passwords
    private Date createdAt;
    private Date updatedAt;

    @DBRef
    private List<UserRole> roles;

    private HashMap<String, HashMap<String, Object>> connections;
    private boolean verified;
    private String verificationCode;
    private Date verificationCodeExpiry;

    // Constructors, getters, setters
    public User() {
        // Default constructor
        this.connections = new HashMap<String, HashMap<String, Object>>();
        this.verified = false;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRole> role) {
        this.roles = role;
    }

    public HashMap<String, HashMap<String, Object>> getConnections() {
        return connections;
    }

    // Custom set methods
    public void addConnection(String key, HashMap value) {
        this.connections.put(key, value);
    }

    public void removeConnection(String key) {
        this.connections.remove(key);
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Date getVerificationCodeExpiry() {
        return verificationCodeExpiry;
    }

    public void setVerificationCodeExpiry(Date verificationCodeExpiry) {
        this.verificationCodeExpiry = verificationCodeExpiry;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}