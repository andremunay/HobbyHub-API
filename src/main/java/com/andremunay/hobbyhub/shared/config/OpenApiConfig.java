package com.andremunay.hobbyhub.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI hobbyHubOpenApi() {
    return new OpenAPI()
        // 1) Basic API metadata
        .info(
            new Info()
                .title("HobbyHub API")
                .version("v1")
                .description("APIs for flashcards and weightlifting modules"))
        // 2) Only your production server URL
        .servers(List.of(new Server().url("https://hobbyhub-api.fly.dev")));
  }
}
