package com.andremunay.hobbyhub.shared.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {

  // Always public: root -> welcome, docs, health checks, OAuth endpoints…
  private static final String[] PUBLIC_MATCHERS = {
    "/", "/welcome",
    "/actuator/**",
    "/oauth2/**",
    "/login/**",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/actuator/prometheus"
  };

  /**
   * Apply CORS as the very first filter.
   */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
    return new CorsFilter((UrlBasedCorsConfigurationSource) corsConfigurationSource);
  }

  /**
   * CORS config: only allow Swagger UI, Stoplight, and same-origin fetches.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOriginPatterns(List.of(
      "https://elements-demo.stoplight.io",
      "https://stoplight.io",
      "https://*.stoplight.io",
      "https://hobbyhub-api.fly.dev"
    ));
    cfg.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "*"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

  /**
   * In local profile, everything is open.
   */
  @Bean
  @Profile("local")
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .authorizeHttpRequests(auth ->
        auth.requestMatchers(PUBLIC_MATCHERS).permitAll()
            .anyRequest().permitAll()
      )
      .headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(
          "default-src 'self'; " +
          "script-src 'self' https://cdn.tailwindcss.com; " +
          "connect-src 'self';"
        ))
        .httpStrictTransportSecurity(hsts -> hsts
          .includeSubDomains(true)
          .maxAgeInSeconds(31_536_000))
        .frameOptions(frame -> frame.deny())
        .xssProtection(xss -> xss.headerValue(
          XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))
      )
      .csrf(csrf -> csrf.disable());
    return http.build();
  }

  /**
   * In fly, GETs are open, POST/DELETE (and any other non-GET)
   * require GitHub OAuth2, CSRF remains disabled.
   */
  @Bean
  @Profile("fly")
  public SecurityFilterChain flyFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .authorizeHttpRequests(auth ->
        auth.requestMatchers(PUBLIC_MATCHERS).permitAll()                    // landing, docs, health…
            .requestMatchers(HttpMethod.GET, "/**").permitAll()              // all GETs open
            .anyRequest().authenticated()                                    // POST, DELETE, etc. locked down
      )
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint(
          new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")
        )
      )
      .oauth2Login(oauth -> oauth
        .loginPage("/oauth2/authorization/github")
        .defaultSuccessUrl("/", true)
      )
      .logout(logout -> logout
        .logoutSuccessUrl("/")
        .permitAll()
      )
      .headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives(
          "default-src 'self'; " +
          "script-src 'self' https://cdn.tailwindcss.com; " +
          "connect-src 'self' https://elements-demo.stoplight.io https://hobbyhub-api.fly.dev;"
        ))
        .httpStrictTransportSecurity(hsts -> hsts
          .includeSubDomains(true)
          .maxAgeInSeconds(31_536_000))
        .frameOptions(frame -> frame.deny())
        .xssProtection(xss -> xss.headerValue(
          XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))
      )
      .csrf(csrf -> csrf.disable());
    return http.build();
  }
}
