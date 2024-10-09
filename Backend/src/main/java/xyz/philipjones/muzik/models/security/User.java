package xyz.philipjones.muzik.models;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;

@Document(collection = "users")
public class User {
    @Id
    private ObjectId id;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String password;  // Store hashed password

    private Date createdAt;

    private Date updatedAt;

    private HashMap<String, HashMap<String, Object>> connections;

    // Constructors, getters, setters
    public User() {
        this.connections = new HashMap<String, HashMap<String, Object>>();
    }

    public User(String username, String firstName, String lastName, String email, String password) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.connections = new HashMap<String, HashMap<String, Object>>();
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public HashMap<String, HashMap<String, Object>> getConnections() {
        return connections;
    }

    public void addConnection(String key, HashMap value) {
        this.connections.put(key, value);
    }

    public void removeConnection(String key) {
        this.connections.remove(key);
    }
}