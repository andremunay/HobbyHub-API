package com.andremunay.hobbyhub.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserContextFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // Set requestId
      MDC.put("requestId", UUID.randomUUID().toString());

      // Set user if authenticated
      Authentication auth = (Authentication) request.getUserPrincipal();
      if (auth instanceof OAuth2AuthenticationToken token) {
        OAuth2User user = token.getPrincipal();
        String githubLogin = user.getAttribute("login");
        if (githubLogin != null) {
          MDC.put("user", githubLogin);
        }
      }

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
