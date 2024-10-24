package xyz.philipjones.muzik;

import org.junit.jupiter.api.Test;
import xyz.philipjones.muzik.models.UnicodeScript;
import xyz.philipjones.muzik.services.random.string.RandomStringService;
import xyz.philipjones.muzik.services.random.string.UnicodeScriptService;

import static org.junit.jupiter.api.Assertions.*;

public class TestFile {

    private UnicodeScriptService unicodeScriptService = new UnicodeScriptService();
    private RandomStringService randomStringService = new RandomStringService(unicodeScriptService);


    @Test
    void testGenerateRandomCharacter() {
        UnicodeScript unicodeScript = unicodeScriptService.generateRandomScript();

        char randomChar = randomStringService.generateRandomCharacter(unicodeScript);
//        System.out.println("Random Char:" + randomChar);
        assertTrue(Character.isDefined(randomChar));
    }

    @Test
    void testGenerateRandomString() {
        UnicodeScript unicodeScript = unicodeScriptService.generateRandomScript();

        String randomString = randomStringService.generateRandomString(unicodeScript);
        System.out.println("Random String:" + randomString);
        assertNotNull(randomString);
        assertTrue(randomString.length() >= 6);
    }

    @Test
    void testSpecificCharExists() {
        char specificChar = (char) 0x3098;
//        System.out.println("Specific Char:" + specificChar);
        assertNotNull(specificChar);
    }
}