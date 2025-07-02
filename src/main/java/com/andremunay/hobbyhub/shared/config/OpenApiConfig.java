package com.andremunay.hobbyhub.shared.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

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
      // 1) Existing metadata + server URL
      .info(new Info()
        .title("HobbyHub API")
        .version("v1")
        .description("APIs for Spanish flashcards and weightlifting modules"))
      .servers(List.of(
        new Server().url("https://hobbyhub-api.fly.dev")
      ))

      // 2) Declare an OAuth2 security‐scheme named “oauth2”
      .components(new Components()
        .addSecuritySchemes("oauth2", new SecurityScheme()
          .type(SecurityScheme.Type.OAUTH2)
          .flows(new OAuthFlows()
            .authorizationCode(new OAuthFlow()
              .authorizationUrl("https://github.com/login/oauth/authorize")
              .tokenUrl("https://github.com/login/oauth/access_token")
              .scopes(new Scopes()
                .addString("repo",      "Access your repositories")
                .addString("read:user", "Read your GitHub profile")
              )
            )
          )
        )
      );
  }

  @Bean
  public OpenApiCustomizer methodSecurityCustomizer() {
    return openApi -> {
      var oauthRequirement = new SecurityRequirement().addList("oauth2");
      openApi.getPaths().forEach((path, item) -> {
        if (item.getPost()   != null) item.getPost().addSecurityItem(oauthRequirement);
        if (item.getPut()    != null) item.getPut().addSecurityItem(oauthRequirement);
        if (item.getDelete() != null) item.getDelete().addSecurityItem(oauthRequirement);
        if (item.getPatch()  != null) item.getPatch().addSecurityItem(oauthRequirement);
      });
    };
  }
}
