package xyz.philipjones.muzik.services.security;

import org.bson.types.ObjectId;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.models.security.UserRole;
import xyz.philipjones.muzik.repositories.UserRepository;
import xyz.philipjones.muzik.repositories.UserRolesRepository;
import xyz.philipjones.muzik.services.email.EmailService;

import java.util.*;

@Service
public class UserService {

    private final String frontendUrl;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringEncryptor stringEncryptor;
    private final EmailService emailService;
    private final UserRolesRepository userRolesRepository;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                       EmailService emailService, @Value("${frontend.https.url}") String frontendUrl, UserRolesRepository userRolesRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.stringEncryptor = stringEncryptor;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.userRolesRepository = userRolesRepository;
    }

    public boolean registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return false; // Username already exists
        } else if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return false; // Email already exists
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        assignDefaultRole(user);

        try {
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean verifyUser(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<UserRole> getRolesByUsername(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        assert user != null;
        return user.getRoles();
    }

    public Optional<User> getUserById(ObjectId id) {
        return userRepository.findById(id);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public String getSpotifyRefreshToken(User user) {
        Map<String, Object> spotifyConnection = getConnections(user, "spotify");
        if (spotifyConnection == null) {
            return null;
        }

        return stringEncryptor.decrypt((String) spotifyConnection.get("refreshToken"));
    }

    public void removeConnection(User user, String connection) {
        user.getConnections().remove(connection);
        userRepository.save(user);
    }

    //-----------------------------------------Private Methods-----------------------------------------

    private Map<String, Object> getConnections(User user, String connectionName) {
        return user.getConnections().get(connectionName);
    }

    private void assignDefaultRole(User user) {
        UserRole defaultRole = userRolesRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(List.of(defaultRole));
    }

    private String generateVerificationCode() {
        return UUID.randomUUID().toString();
    }
}
