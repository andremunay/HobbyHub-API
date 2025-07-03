package com.andremunay.hobbyhub.shared.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {

  private static final String[] PUBLIC_MATCHERS = {
    "/",
    "/welcome",
    "/actuator/**",
    "/oauth2/**",
    "/login/**",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/swagger-ui.html",
    "/actuator/prometheus"
  };

  // 1) Install CORS as first filter
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public CorsFilter corsFilter(UrlBasedCorsConfigurationSource corsSource) {
    return new CorsFilter(corsSource);
  }

  // 2) CORS rules: only allow Swagger UI, Stoplight, same‐origin
  @Bean
  public UrlBasedCorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOriginPatterns(
        List.of(
            "https://hobbyhub-api.fly.dev",
            "https://elements-demo.stoplight.io",
            "https://stoplight.io",
            "https://*.stoplight.io"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "*"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

  // 3) In local/dev mode, everything is open
  @Bean
  @Profile("local")
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http.cors(c -> c.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            a -> a.requestMatchers(PUBLIC_MATCHERS).permitAll().anyRequest().permitAll())
        .headers(
            h ->
                h.contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' https://cdn.tailwindcss.com; "
                                    + "connect-src 'self';"))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    .frameOptions(f -> f.deny())
                    .xssProtection(
                        x ->
                            x.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
        .csrf(c -> c.disable());
    return http.build();
  }

  // 4) In prod/fly mode: GETs & public matchers are open, others need OAuth2
  @Bean
  @Profile("fly")
  public SecurityFilterChain flyFilterChain(HttpSecurity http) throws Exception {
    // matcher for JSON API calls (Accept: application/json)
    var jsonMatcher =
        new MediaTypeRequestMatcher(
            new HeaderContentNegotiationStrategy(), MediaType.APPLICATION_JSON);

    http.cors(c -> c.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(PUBLIC_MATCHERS)
                    .permitAll() // /, /welcome, docs, health…
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll() // all GETs
                    .anyRequest()
                    .authenticated() // POST/PUT/DELETE…
            )
        .exceptionHandling(
            e ->
                e
                    // for JSON clients: return 401
                    .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), jsonMatcher)
                    // for browsers/UI: redirect into OAuth flow
                    .authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")))
        .oauth2Login(
            o -> o.loginPage("/oauth2/authorization/github").defaultSuccessUrl("/welcome", true))
        .logout(l -> l.logoutSuccessUrl("/").permitAll())
        .headers(
            h ->
                h.contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' https://cdn.tailwindcss.com; "
                                    + "connect-src 'self' https://hobbyhub-api.fly.dev https://elements-demo.stoplight.io;"))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    .frameOptions(f -> f.deny())
                    .xssProtection(
                        x ->
                            x.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
        .csrf(c -> c.disable());
    return http.build();
  }
}
