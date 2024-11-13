package xyz.philipjones.muzik.models.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "userRoles")
public class UserRole {

    @Id
    private String id;
    private String name; // Role name, e.g., "ROLE_USER", "ROLE_ADMIN"
    private List<String> permissions;

    // Constructors, getters, setter
    public UserRole() {}

    public UserRole(String name, List<String> permissions) {
        this.name = name;
        this.permissions = permissions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
