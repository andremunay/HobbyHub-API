package com.andremunay.hobbyhub.shared.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
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
