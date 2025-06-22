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

/**
 * Servlet filter that enriches logs with request-scoped context using MDC (Mapped Diagnostic
 * Context).
 *
 * <p>Injects a unique request ID and (if authenticated) the GitHub username into the logging
 * context. These MDC entries are automatically included in log statements if configured in the log
 * pattern.
 */
@Component
public class UserContextFilter extends OncePerRequestFilter {

  /**
   * Adds MDC context (request ID and GitHub user) to the logging scope for the current request.
   *
   * <p>Clears MDC afterward to prevent thread leakage in concurrent request handling.
   *
   * @param request the incoming HTTP request
   * @param response the outgoing HTTP response
   * @param filterChain the remaining filter chain
   * @throws ServletException if filtering fails
   * @throws IOException if I/O fails during processing
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // Generate and store a unique request ID for traceability
      MDC.put("requestId", UUID.randomUUID().toString());

      // If authenticated via OAuth2, extract the GitHub login name for log enrichment
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
      // Clean up MDC to avoid leaking context across threads
      MDC.clear();
    }
  }
}
