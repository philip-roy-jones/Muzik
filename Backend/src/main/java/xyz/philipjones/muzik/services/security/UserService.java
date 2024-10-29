package xyz.philipjones.muzik.services.security;

import org.bson.types.ObjectId;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
// TODO: refactor to use the service instead
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.repositories.UserRepository;
import xyz.philipjones.muzik.services.email.EmailService;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final String frontendUrl;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringEncryptor stringEncryptor;
    private final EmailService emailService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor,
                       EmailService emailService, @Value("${frontend.http.url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.stringEncryptor = stringEncryptor;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public boolean registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return false; // Username already exists
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add("unverified");

        try {
            userRepository.save(user);
            sendVerificationCode(user);
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
            user.getRoles().remove("unverified");
            user.getRoles().add("user");
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
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

    private void sendVerificationCode(User user) {
        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiry(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)); // 24 hours expiry
        userRepository.save(user);

        String verificationUrl = frontendUrl + "/verify?code=" + verificationCode;
        String emailText = "Please verify your email by clicking the following link: " + verificationUrl;
        emailService.sendEmail(user.getEmail(), "Muzik Email Verification", emailText);
    }

    private String generateVerificationCode() {
        return UUID.randomUUID().toString();
    }
}
