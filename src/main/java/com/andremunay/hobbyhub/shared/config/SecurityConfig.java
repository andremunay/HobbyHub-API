package com.andremunay.hobbyhub.shared.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**", "/login/**", "/oauth2/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exception ->
                exception.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")))
        .oauth2Login(oauth -> oauth.defaultSuccessUrl("/welcome", true))
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

    return http.build();
  }
}
