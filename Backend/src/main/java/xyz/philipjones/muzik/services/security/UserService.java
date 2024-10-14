package xyz.philipjones.muzik.services.security;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringEncryptor stringEncryptor;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, @Qualifier("jasyptStringEncryptor") StringEncryptor stringEncryptor) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.stringEncryptor = stringEncryptor;
    }

    public boolean registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return false; // Username already exists
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            return userRepository.save(user) != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public String getSpotifyRefreshToken(User user) {
        return stringEncryptor.decrypt((String) user.getConnections().get("spotify").get("refreshToken"));
    }
}
