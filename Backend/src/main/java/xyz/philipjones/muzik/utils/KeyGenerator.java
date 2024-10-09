package xyz.philipjones.muzik.utils;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;

public class KeyGenerator {
    public static void main(String[] args) throws IOException {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        try (FileOutputStream fos = new FileOutputStream("secret.key")) {
            fos.write(key.getEncoded());
        }
        System.out.println("Key generated and saved to " + new java.io.File("secret.key").getAbsolutePath());
    }
}