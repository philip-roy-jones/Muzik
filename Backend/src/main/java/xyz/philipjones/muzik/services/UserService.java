package xyz.philipjones.muzik.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.User;
import xyz.philipjones.muzik.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean registerUser(User user) {
        // Encrypt the password before saving
        System.out.println("test");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        System.out.println("test2");
        try {
            boolean result = userRepository.save(user) != null;
            System.out.println("Inner function call: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User signInUser(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPassword())) {
            return userOptional.get();
        }
        return null;
    }
}
