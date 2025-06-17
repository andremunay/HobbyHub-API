package com.andremunay.hobbyhub;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Simple test to verify that unauthenticated requests are redirected to GitHub OAuth2 login. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("fly")
class SecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;

  /**
   * Hitting any protected URL without authentication should send a 302 redirect to
   * /oauth2/authorization/github
   */
  @Test
  void whenNotAuthenticated_thenRedirectToGithubLogin() throws Exception {
    mockMvc
        .perform(get("/flashcards"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/oauth2/authorization/github"));
  }

  /**
   * If we hit an allowed endpoint (like /actuator/health), we should get 200 OK without redirect.
   */
  @Test
  void whenHitActuatorHealth_thenOk() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  /**
   * If we simulate a logged‐in user, we should be able to hit “/” without redirect. We
   * use @WithMockUser to bypass real OAuth.
   */
  @Test
  void whenAuthenticated_thenAccessWelcomePage() throws Exception {
    Map<String, Object> attributes = Map.of("name", "Test User");

    var principal = new DefaultOAuth2User(List.of(() -> "ROLE_USER"), attributes, "name");

    var auth = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");

    mockMvc
        .perform(get("/welcome").with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "<h1 class=\"text-3xl font-bold mb-6\">Welcome to HobbyHub API</h1>")));
  }

  /** Test logout flows to “/” after signing out. */
  @Test
  @WithMockUser(username = "testuser")
  void whenLogout_thenRedirectToHome() throws Exception {
    mockMvc
        .perform(logout("/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }
}
