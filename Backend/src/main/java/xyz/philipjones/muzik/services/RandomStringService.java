package xyz.philipjones.muzik.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.UnicodeScript;

import java.util.List;
import java.util.Random;

@Service
public class RandomStringService {

    private final UnicodeScriptService unicodeScriptService;

    @Autowired
    public RandomStringService(UnicodeScriptService unicodeScriptService) {
        this.unicodeScriptService = unicodeScriptService;
    }

    public char generateRandomCharacter(UnicodeScript unicodeScript) {
        List<String> randomRange = unicodeScriptService.generateRandomRange(unicodeScript);

        int unicodeStart = Integer.parseInt(randomRange.get(0), 16);
        int unicodeEnd = Integer.parseInt(randomRange.get(1), 16);
//        System.out.println("Unicode Start:" + unicodeStart);
//        System.out.println("Unicode End:" + unicodeEnd);
        Random random = new Random();
        return (char) (random.nextInt(unicodeEnd - unicodeStart + 1) + unicodeStart);
    }

    public String generateRandomString(UnicodeScript unicodeScript) {

        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        int minLength = 1;
        int maxLength = 5;
        int length = random.nextInt(maxLength - minLength + 1) + minLength;
//        System.out.println("-----------------------------------");
        while (sb.length() < length) {
            char randomChar = generateRandomCharacter(unicodeScript);
            sb.append(randomChar);
        }
//        System.out.println("-----------------------------------");
        return sb.toString().trim();
    }
}