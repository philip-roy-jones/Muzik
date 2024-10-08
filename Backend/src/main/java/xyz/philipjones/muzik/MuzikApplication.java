package xyz.philipjones.muzik;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MuzikApplication {

	public static void main(String[] args) {
		SpringApplication.run(MuzikApplication.class, args);
	}
}