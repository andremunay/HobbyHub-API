package com.andremunay.hobbyhub;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Test configuration for spinning up a PostgreSQL container using Testcontainers.
 *
 * <p>This configuration: - Loads environment variables from a `.env` file using dotenv - Registers
 * a singleton PostgreSQLContainer bean for test database access - Automatically connects Spring's
 * DataSource via @ServiceConnection
 *
 * <p>This setup is shared across integration and repository tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
  static {
    // Load .env file into system properties (for use in @Value fields)
    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
  }

  @Value("${POSTGRES_DB}")
  private String databaseName;

  @Value("${POSTGRES_USER}")
  private String username;

  @Value("${POSTGRES_PASSWORD}")
  private String password;

  /**
   * Creates and configures a PostgreSQL container with credentials from environment. Uses
   * PostgreSQL 15 image and integrates with Spring Boot via @ServiceConnection.
   *
   * @return a configured PostgreSQLContainer instance
   */
  @SuppressWarnings("resource")
  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName(databaseName)
        .withUsername(username)
        .withPassword(password);
  }
}
