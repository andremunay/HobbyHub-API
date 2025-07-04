package com.andremunay.hobbyhub.shared.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
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

  /** 1) CORS filter at highest precedence, wired from corsConfigurationSource(). */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public CorsFilter corsFilter(UrlBasedCorsConfigurationSource corsSource) {
    return new CorsFilter(corsSource);
  }

  /** 2) CORS rules: only allow your UI origins + same‐origin. */
  @Bean
  public UrlBasedCorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOriginPatterns(List.of("https://hobbyhub-api.fly.dev"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "*"));
    cfg.setAllowCredentials(true);

    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }

  /** 3) Local/dev: everything open, no CSRF, with security headers. */
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
                        xss ->
                            xss.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
        .csrf(c -> c.disable());

    return http.build();
  }

  /**
   * 4) Prod/fly: GETs & PUBLIC_MATCHERS are open; POST/PUT/DELETE → 401 for API clients, 302→GitHub
   * for browsers.
   */
  @Bean
  @Profile("fly")
  public SecurityFilterChain flyFilterChain(HttpSecurity http) throws Exception {
    // any write operation → catch and send 401
    RequestMatcher apiWrites =
        new OrRequestMatcher(
            new AntPathRequestMatcher("/**", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/**", HttpMethod.PUT.name()),
            new AntPathRequestMatcher("/**", HttpMethod.DELETE.name()));

    http.cors(c -> c.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(PUBLIC_MATCHERS)
                    .permitAll() // landing, docs, health…
                    .requestMatchers(HttpMethod.GET, "/**")
                    .permitAll() // all GETs open
                    .anyRequest()
                    .authenticated() // others locked down
            )
        .exceptionHandling(
            e ->
                e
                    // 401 for XHR/JSON API calls
                    .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiWrites)
                    // 302 redirect for browsers/UI
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
                                    + "connect-src 'self' https://hobbyhub-api.fly.dev;"))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                    .frameOptions(f -> f.deny())
                    .xssProtection(
                        xss ->
                            xss.headerValue(
                                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
        .csrf(c -> c.disable());

    return http.build();
  }
}
