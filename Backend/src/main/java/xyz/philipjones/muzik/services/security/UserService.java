package xyz.philipjones.muzik.services.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.security.User;
import xyz.philipjones.muzik.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public User loginUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPassword())) {
            return userOptional.get();
        }
        return null;
    }
}
