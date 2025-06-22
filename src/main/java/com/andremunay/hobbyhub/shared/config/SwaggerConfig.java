package com.andremunay.hobbyhub.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures server URL overrides for Swagger-generated OpenAPI documentation.
 *
 * <p>Clears default localhost servers and sets the production deployment URL to match the
 * public-facing HobbyHub API on Fly.io.
 */
@Configuration
public class SwaggerConfig {

  /**
   * Removes default server entries (e.g. localhost) from the OpenAPI document.
   *
   * <p>Prevents incorrect base URLs from appearing in generated Swagger clients.
   *
   * @return a customizer that clears server definitions
   */
  @Bean
  public OpenApiCustomizer customizeScheme() {
    return openApi -> openApi.getServers().clear(); // clear default
  }

  /**
   * Registers the production deployment server for Swagger UI and client code generation.
   *
   * @return OpenAPI instance with a single server pointing to Fly.io-hosted API
   */
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI().addServersItem(new Server().url("https://hobbyhub.fly.dev"));
  }
}
