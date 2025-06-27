package com.andremunay.hobbyhub.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Shared security configuration with profile-specific filter chains.
 *
 * <p>Local profile: disables auth and CSRF, permits all requests. Fly profile: enforces OAuth2
 * login, secures endpoints, disables CSRF.
 */
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

  /** Local (development) security: all requests permitted, CSRF disabled, basic CSP. */
  @Bean
  @Profile("local")
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_MATCHERS).permitAll().anyRequest().permitAll())
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; script-src 'self' https://cdn.tailwindcss.com")))
        .csrf(csrf -> csrf.disable());

    return http.build();
  }

  /** Fly (production) security: OAuth2 login, authenticated access, CSRF disabled, CSP. */
  @Bean
  @Profile("fly")
  public SecurityFilterChain flyFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_MATCHERS).permitAll().anyRequest().authenticated())
        // Use the same entry point, but also declare it as your loginPage
        .exceptionHandling(
            exc ->
                exc.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")))
        .oauth2Login(
            oauth ->
                oauth
                    // tells Spring “start the OAuth flow here”
                    .loginPage("/oauth2/authorization/github")
                    .defaultSuccessUrl("/welcome", true))
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; "
                                + "script-src 'self' https://cdn.tailwindcss.com; "
                                + "connect-src 'self' https://hobbyhub-api.fly.dev")))
        .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
