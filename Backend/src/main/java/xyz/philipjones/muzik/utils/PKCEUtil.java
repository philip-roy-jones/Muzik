package xyz.philipjones.muzik.utils;

import java.security.SecureRandom;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PKCEUtil {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateCodeVerifier(int length) {
        if (length < 43 || length > 128) {
            throw new IllegalArgumentException("Length must be between 43 and 128 characters");
        }
        StringBuilder codeVerifier = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            codeVerifier.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return codeVerifier.toString();
    }

    public static String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}