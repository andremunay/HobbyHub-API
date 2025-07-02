package com.andremunay.hobbyhub;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for verifying security behavior under the "fly" profile.
 *
 * <p>These tests validate: - Authentication requirements for protected routes - Redirect behavior
 * for unauthenticated users - Access to public endpoints like Actuator - Login and logout flow with
 * OAuth2
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("fly")
class SecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;

/**
 * Verifies that unauthenticated users are redirected to GitHub OAuth login when accessing
 * protected write endpoints.
 */
@Test
void whenNotAuthenticated_thenRedirectToGithubLoginOnWrite() throws Exception {
    mockMvc.perform(post("/flashcards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "front": "Test question?",
                  "back": "Test answer."
                }
                """))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/oauth2/authorization/github"));
}


  /** Confirms that the /actuator/health endpoint is publicly accessible. */
  @Test
  void whenHitActuatorHealth_thenOk() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  /** Ensures authenticated users can access the welcome page and receive the expected content. */
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

  /** Verifies that calling /logout results in a redirect to the home page. */
  @Test
  @WithMockUser(username = "testuser")
  void whenLogout_thenRedirectToHome() throws Exception {
    mockMvc
        .perform(logout("/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }
  /** Assert that reads remain open */
  @Test
  void whenNotAuthenticated_thenGetIsOk() throws Exception {
      mockMvc.perform(get("/flashcards"))
          .andExpect(status().isOk());
  }
}
