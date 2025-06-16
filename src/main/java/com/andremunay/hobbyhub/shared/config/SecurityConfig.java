package com.andremunay.hobbyhub.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Profile("fly")
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                    .authenticated())
        .exceptionHandling(
            exception ->
                exception.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")))
        .oauth2Login(oauth -> oauth.defaultSuccessUrl("/welcome", true))
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
        .csrf(csrf -> csrf.disable());

    return http.build();
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
      }
    };
  }
}
