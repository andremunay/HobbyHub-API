package com.andremunay.hobbyhub;

import org.springframework.boot.SpringApplication;

/**
 * Custom entry point for running the HobbyHub application with test-specific configuration.
 *
 * <p>This bootstraps the main {@link HobbyhubApplication} and includes {@link
 * TestcontainersConfiguration} to ensure a PostgreSQL container is available during integration
 * testing or local debug runs.
 */
public class TestHobbyhubApiApplication {

  public static void main(String[] args) {
    SpringApplication.from(HobbyhubApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
