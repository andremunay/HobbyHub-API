package com.andremunay.hobbyhub.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("local")
@Configuration
public class LocalSecurityConfig {

  @Bean
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/**",
                        "/login/**",
                        "/oauth2/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/actuator/prometheus")
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; script-src 'self' https://cdn.tailwindcss.com")))
        .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
