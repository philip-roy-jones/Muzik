package xyz.philipjones.muzik.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.philipjones.muzik.models.UnicodeRange;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

@Service
public class StringRandomService {

    private final UnicodeRangeService unicodeRangeService;

    @Autowired
    public StringRandomService(UnicodeRangeService unicodeRangeService) {
        this.unicodeRangeService = unicodeRangeService;
    }

    public char generateRandomCharacter() {
        UnicodeRange unicodeGroup = unicodeRangeService.generateRandomRangeGroup();
        List<String> randomRange = unicodeRangeService.generateRandomRange(unicodeGroup);
        int unicodeStart = Integer.parseInt(randomRange.get(0), 16);
        int unicodeEnd = Integer.parseInt(randomRange.get(1), 16);
        System.out.println("Unicode Start:" + unicodeStart);
        System.out.println("Unicode End:" + unicodeEnd);
        Random random = new Random();
        return (char) (random.nextInt(unicodeEnd - unicodeStart + 1) + unicodeStart);
    }

    public String generateRandomString() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        int minLength = 32;
        int maxLength = 64;
        int length = random.nextInt(maxLength - minLength + 1) + minLength;
        System.out.println("-----------------------------------");
        for (int i = 0; i < length; i++) {
            char randomChar = generateRandomCharacter();
            sb.append(randomChar);
        }
        System.out.println("-----------------------------------");
        return sb.toString().trim();
    }
}