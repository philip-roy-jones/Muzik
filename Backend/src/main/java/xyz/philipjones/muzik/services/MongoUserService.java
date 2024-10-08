package xyz.philipjones.muzik.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.MongoUser;
import xyz.philipjones.muzik.models.RedisUser;
import xyz.philipjones.muzik.repositories.MongoUserRepository;

import java.util.Date;

@Service
public class MongoUserService {
    @Autowired
    private MongoUserRepository mongoUserRepository;

    public void updateToken(MongoUser mongoUser, String newRefreshToken) {
        mongoUser.setRefreshToken(newRefreshToken);
        mongoUser.setUpdatedAt(new Date());
        mongoUserRepository.save(mongoUser);
    }

    public void createRefreshToken (String refreshToken) {
        MongoUser mongoUser = new MongoUser();
        mongoUser.setRefreshToken(refreshToken);
        mongoUser.setCreatedAt(new Date());
        mongoUser.setUpdatedAt(new Date());
        mongoUserRepository.save(mongoUser);
    }
}