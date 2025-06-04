package com.andremunay.hobbyhub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Value("${POSTGRES_DB:hobbyhub}")
  private String databaseName;

  @Value("${POSTGRES_USER:hobbyuser}")
  private String username;

  @Value("${POSTGRES_PASSWORD:hobbysecret}")
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
