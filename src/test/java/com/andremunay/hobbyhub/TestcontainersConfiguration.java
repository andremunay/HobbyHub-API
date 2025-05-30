package com.andremunay.hobbyhub;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("hobbyhub")
        .withUsername("hobbyuser")
        .withPassword("hobbysecret");
  }
}
