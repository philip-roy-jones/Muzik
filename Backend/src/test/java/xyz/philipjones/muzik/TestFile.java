package xyz.philipjones.muzik;

import org.junit.jupiter.api.Test;
import xyz.philipjones.muzik.services.StringRandomService;
import xyz.philipjones.muzik.services.UnicodeRangeService;

import static org.junit.jupiter.api.Assertions.*;

public class TestFile {

    private UnicodeRangeService unicodeRangeService = new UnicodeRangeService();
    private StringRandomService stringRandomService = new StringRandomService(unicodeRangeService);


    @Test
    void testGenerateRandomCharacter() {
        char randomChar = stringRandomService.generateRandomCharacter();
        System.out.println("Random Char:" + randomChar);
        assertTrue(Character.isDefined(randomChar));
    }

    @Test
    void testGenerateRandomString() {
        String randomString = stringRandomService.generateRandomString();
        System.out.println("Random String:" + randomString);
        assertNotNull(randomString);
        assertTrue(randomString.length() >= 6);
    }

    @Test
    void testSpecificCharExists() {
        char specificChar = (char) 0x3098;
        System.out.println("Specific Char:" + specificChar);
        assertNotNull(specificChar);
    }
}