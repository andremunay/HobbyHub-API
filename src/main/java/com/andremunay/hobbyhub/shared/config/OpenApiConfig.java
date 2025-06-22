package com.andremunay.hobbyhub.shared.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures metadata for the generated OpenAPI documentation.
 *
 * <p>Sets title, version, and description for display in Swagger UI and other API tooling.
 */
@Configuration
public class OpenApiConfig {

  /**
   * Customizes the OpenAPI specification with application metadata.
   *
   * @return an {@link OpenApiCustomizer} that adds title, version, and description
   */
  @Bean
  OpenApiCustomizer customizer() {
    return openApi ->
        openApi.info(
            new Info()
                .title("HobbyHub API")
                .version("v1")
                .description("APIs for Spanish flashcards and weightlifting modules"));
  }
}
