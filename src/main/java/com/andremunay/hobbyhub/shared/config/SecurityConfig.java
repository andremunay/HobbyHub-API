package com.andremunay.hobbyhub.shared.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  private static final String[] PUBLIC_MATCHERS = {
    "/actuator/**",
    "/login/**",
    "/oauth2/**",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/actuator/prometheus"
  };

  /**
   * CORS config: allow Stoplight UI.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(
      "https://elements-demo.stoplight.io",
      "https://stoplight.io"
    ));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  @Bean
  @Profile("local")
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http
      // apply our CORS rules
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))

      // permit everything
      .authorizeHttpRequests(auth ->
        auth.requestMatchers(PUBLIC_MATCHERS).permitAll()
            .anyRequest().permitAll()
      )

      // basic CSP and no CSRF
      .headers(headers ->
        headers.contentSecurityPolicy(csp ->
          csp.policyDirectives(
            "default-src 'self'; " +
            "script-src 'self' https://cdn.tailwindcss.com; " +
            "connect-src 'self';"
          )
        )
      )
      .csrf(csrf -> csrf.disable());

    return http.build();
  }

  @Bean
  @Profile("fly")
  public SecurityFilterChain flyFilterChain(HttpSecurity http) throws Exception {
    http
      // apply CORS again in prod
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))

      // public endpoints vs. everything else
      .authorizeHttpRequests(auth ->
        auth.requestMatchers(PUBLIC_MATCHERS).permitAll()
            .anyRequest().authenticated()
      )

      // kick unauthenticated users into the GitHub OAuth flow
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint(
          new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")
        )
      )
      .oauth2Login(oauth -> oauth
        .loginPage("/oauth2/authorization/github")
        .defaultSuccessUrl("/welcome", true)
      )
      .logout(logout -> logout
        .logoutSuccessUrl("/")
        .permitAll()
      )

      // CSP
      .headers(headers ->
        headers.contentSecurityPolicy(csp ->
          csp.policyDirectives(
            "default-src 'self'; " +
            "script-src 'self' https://cdn.tailwindcss.com; " +
            "connect-src 'self';"
          )
        )
      )

      // no CSRF in API mode
      .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
