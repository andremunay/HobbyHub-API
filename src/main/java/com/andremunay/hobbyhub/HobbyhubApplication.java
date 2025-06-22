package com.andremunay.hobbyhub;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the HobbyHub Spring Boot application.
 *
 * <p>Loads environment variables from a `.env` file (if present) and launches the application
 * context.
 */
@Slf4j
@SpringBootApplication
public class HobbyhubApplication {

  public static void main(String[] args) {
    // Load .env values into system properties if file exists
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

    SpringApplication.run(HobbyhubApplication.class, args);
  }
}
