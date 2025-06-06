package com.andremunay.hobbyhub;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class HobbyhubApplication {

  public static void main(String[] args) {
    // Load .env file
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

    SpringApplication.run(HobbyhubApplication.class, args);
  }
}
