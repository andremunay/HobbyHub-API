package com.andremunay.hobbyhub.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenApiCustomizer customizeScheme() {
    return openApi -> openApi.getServers().clear(); // clear default
  }

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI().addServersItem(new Server().url("https://hobbyhub.fly.dev"));
  }
}
