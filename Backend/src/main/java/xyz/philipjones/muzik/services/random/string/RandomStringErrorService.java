package xyz.philipjones.muzik.services.random.string;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.RandomStringError;
import xyz.philipjones.muzik.repositories.RandomStringErrorRepository;

@Service
public class RandomStringErrorService {

    private final RandomStringErrorRepository randomStringErrorRepository;

    @Autowired
    public RandomStringErrorService(RandomStringErrorRepository randomStringErrorRepository) {
        this.randomStringErrorRepository = randomStringErrorRepository;
    }

    public void saveRandomStringError(String randomString) {
        RandomStringError randomStringError = new RandomStringError();
        randomStringError.setRandomString(randomString);
        randomStringErrorRepository.save(randomStringError);
    }
}
