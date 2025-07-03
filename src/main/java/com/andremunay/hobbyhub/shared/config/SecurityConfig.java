package com.andremunay.hobbyhub.shared.config;

import java.util.List;
import java.util.Set;
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

  /** 1) CORS filter at the highest precedence, wired from corsConfigurationSource(). */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public CorsFilter corsFilter(UrlBasedCorsConfigurationSource corsSource) {
    return new CorsFilter(corsSource);
  }

  /** 2) CORS rules: only allow your UI origins + same‐origin. */
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

    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

  /** 3) Local/dev: everything is open, no CSRF, with security headers. */
  @Bean
  @Profile("local")
  public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_MATCHERS).permitAll().anyRequest().permitAll())
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' https://cdn.tailwindcss.com; "
                                    + "connect-src 'self';"))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    .frameOptions(f -> f.deny())
                    .xssProtection(
                        xss ->
                            xss.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    .referrerPolicy(rp -> rp.policy(ReferrerPolicy.NO_REFERRER)))
        .csrf(csrf -> csrf.disable());

    return http.build();
  }

  /**
   * 4) Fly/production: GETs & public paths are open; non-GETs require OAuth2.
   *
   * <p>- XHR/JSON clients get a 401 - Browsers/UI get a 302 → GitHub OAuth
   */
  @Bean
  @Profile("fly")
  public SecurityFilterChain flyFilterChain(HttpSecurity http) throws Exception {
    // match Accept: application/json
    var jsonMatcher =
        new MediaTypeRequestMatcher(
            new HeaderContentNegotiationStrategy(), MediaType.APPLICATION_JSON);
    jsonMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_MATCHERS)
                    .permitAll() // landing, docs, health…
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll() // all GETs
                    .anyRequest()
                    .authenticated() // POST/PUT/DELETE…
            )
        .exceptionHandling(
            ex ->
                ex
                    // 401 for JSON/XHR
                    .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), jsonMatcher)
                    // 302 redirect for browsers
                    .authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/github")))
        .oauth2Login(
            oauth ->
                oauth.loginPage("/oauth2/authorization/github").defaultSuccessUrl("/welcome", true))
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' https://cdn.tailwindcss.com; "
                                    + "connect-src 'self' "
                                    + "https://hobbyhub-api.fly.dev "
                                    + "https://elements-demo.stoplight.io "
                                    + "https://github.com "
                                    + "https://github.com/login/oauth/authorize "
                                    + "https://github.com/login/oauth/access_token;"))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    .frameOptions(f -> f.deny())
                    .xssProtection(
                        xss ->
                            xss.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    .referrerPolicy(rp -> rp.policy(ReferrerPolicy.NO_REFERRER)))
        .csrf(csrf -> csrf.disable());

    return http.build();
  }
}
