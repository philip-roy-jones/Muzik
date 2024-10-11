package xyz.philipjones.muzik.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.salt.FixedStringSaltGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("${jasypt.encryptor.password}");
        encryptor.setAlgorithm("PBEWithMD5AndDES");

        FixedStringSaltGenerator saltGenerator = new FixedStringSaltGenerator();
        saltGenerator.setSalt("${jasypt.encryptor.salt}");
        encryptor.setSaltGenerator(saltGenerator);

        return encryptor;
    }
}