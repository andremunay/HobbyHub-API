package com.andremunay.hobbyhub.shared.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
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

  /** CORS config: only allow your Swagger UI, Stoplight, and same-origin. */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(
        List.of(
            "https://elements-demo.stoplight.io",
            "https://stoplight.io",
            "https://hobbyhub-api.fly.dev"));
    cfg.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  @Bean
  @Profile("local")
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_MATCHERS).permitAll().anyRequest().permitAll())
        .headers(
            headers ->
                headers
                    // tighten your CSP
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' https://cdn.tailwindcss.com; "
                                    + "connect-src 'self';"))
                    // HSTS: one year, include subdomains
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                    // clickjacking protection
                    .frameOptions(frame -> frame.deny())
                    // enable XSS Auditor in block mode
                    .xssProtection(
                        xss ->
                            xss.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    // never send a Referer
                    .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER)))
        .csrf(csrf -> csrf.disable());

    return http.build();
  }

  @Bean
  @Profile("fly")
  public SecurityFilterChain flyFilterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_MATCHERS).permitAll().anyRequest().authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")))
        .oauth2Login(
            oauth ->
                oauth.loginPage("/oauth2/authorization/github").defaultSuccessUrl("/welcome", true))
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
        // stateful CSRF kept enabled by not disabling it
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' https://cdn.tailwindcss.com; "
                                    + "connect-src 'self' https://elements-demo.stoplight.io https://hobbyhub-api.fly.dev;"))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                    .frameOptions(frame -> frame.deny())
                    .xssProtection(
                        xss ->
                            xss.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER)));

    return http.build();
  }
}
