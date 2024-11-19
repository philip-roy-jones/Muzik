package xyz.philipjones.muzik.seeders;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import xyz.philipjones.muzik.models.security.UserRole;
import xyz.philipjones.muzik.repositories.UserRolesRepository;

@Component
public class UserRoleSeeder {

    @Autowired
    private UserRolesRepository userRolesRepository;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            if (userRolesRepository.count() == 0) {
                userRolesRepository.save(new UserRole("ROLE_ADMIN"));
                userRolesRepository.save(new UserRole("ROLE_USER"));
            }
        };
    }
}
