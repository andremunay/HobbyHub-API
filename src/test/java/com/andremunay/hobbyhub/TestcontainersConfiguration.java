package com.andremunay.hobbyhub;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
  static {
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
  }

  @Value("${POSTGRES_DB}")
  private String databaseName;

  @Value("${POSTGRES_USER}")
  private String username;

  @Value("${POSTGRES_PASSWORD}")
  private String password;

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName(databaseName)
        .withUsername(username)
        .withPassword(password);
  }
}
