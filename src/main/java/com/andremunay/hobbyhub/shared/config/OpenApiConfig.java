package com.andremunay.hobbyhub.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures metadata for the generated OpenAPI documentation.
 *
 * <p>Sets title, version, and description for display in Swagger UI and other API tooling.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI hobbyHubOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("HobbyHub API")
                .version("v1")
                .description("APIs for Spanish flashcards and weightlifting modules"))
        .servers(List.of(new Server().url("https://hobbyhub-api.fly.dev")));
  }
}
