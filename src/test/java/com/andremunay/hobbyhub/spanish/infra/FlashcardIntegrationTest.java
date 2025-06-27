package com.andremunay.hobbyhub.spanish.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for Flashcard REST API, verifying end-to-end behavior.
 *
 * <p>Tests are run with a real Spring context, Testcontainers PostgreSQL, and real HTTP wiring via
 * MockMvc.
 */
@AutoConfigureMockMvc
@Import(com.andremunay.hobbyhub.TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
class FlashcardIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  /** Full test flow: POST → GET due cards → validate date. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void fullFlow_postAndReview() throws Exception {
    // 1) Create a new flashcard
    String flashcardJson = "{\"front\":\"test\",\"back\":\"test\"}";
    mockMvc
        .perform(
            post("/flashcards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(flashcardJson))
        .andExpect(status().isOk());

    // 2) Fetch only due flashcards via the /review endpoint
    MvcResult result =
        mockMvc
            .perform(
                get("/flashcards/review").param("due", "true").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    // 3) Parse and assert
    String json = result.getResponse().getContentAsString();
    FlashcardReviewDto[] reviews = mapper.readValue(json, FlashcardReviewDto[].class);

    assertThat(reviews).isNotEmpty();
    assertThat(reviews[0].getNextReviewOn()).isAfterOrEqualTo(LocalDate.now());
  }

  /** Verifies that multiple flashcards can be created and retrieved. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void getAll_returnsCreatedFlashcards() throws Exception {
    String card1 = "{\"front\":\"hola\",\"back\":\"hello\"}";
    String card2 = "{\"front\":\"adios\",\"back\":\"goodbye\"}";
    mockMvc
        .perform(
            post("/flashcards").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(card1))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/flashcards").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(card2))
        .andExpect(status().isOk());

    MvcResult mvc =
        mockMvc
            .perform(get("/flashcards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    FlashcardReviewDto[] all =
        mapper.readValue(mvc.getResponse().getContentAsString(), FlashcardReviewDto[].class);
    assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    List<String> fronts = Arrays.stream(all).map(FlashcardReviewDto::getFront).toList();
    assertThat(fronts)
        .as("fronts should include at least our two new cards")
        .contains("hola", "adios");
  }

  /** Tests that submitting a review with a grade updates the review schedule. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void reviewEndpoint_updatesNextReviewBasedOnGrade() throws Exception {
    // 1) Create a flashcard
    String create = "{\"front\":\"uno\",\"back\":\"one\"}";
    mockMvc
        .perform(
            post("/flashcards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(create))
        .andExpect(status().isOk());

    // 2) List all flashcards to retrieve the front value
    MvcResult listRes =
        mockMvc
            .perform(get("/flashcards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    FlashcardReviewDto[] all =
        mapper.readValue(listRes.getResponse().getContentAsString(), FlashcardReviewDto[].class);
    String front = all[0].getFront();

    // 3) Submit a review using the front text and grade
    String gradeJson = String.format("{\"front\":\"%s\",\"grade\":5}", front);
    MvcResult revRes =
        mockMvc
            .perform(
                post("/flashcards/review")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gradeJson))
            .andExpect(status().isOk())
            .andReturn();

    // 4) Parse and assert that nextReviewOn is updated
    FlashcardReviewDto updated =
        mapper.readValue(revRes.getResponse().getContentAsString(), FlashcardReviewDto.class);

    assertThat(updated.getNextReviewOn()).isAfter(LocalDate.now());
  }

  /** Verifies that a flashcard can be deleted and no longer appears in results. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void deleteCard_succeeds() throws Exception {
    // 1) Create a new flashcard
    String create = "{\"front\":\"dos\",\"back\":\"two\"}";
    mockMvc
        .perform(
            post("/flashcards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(create))
        .andExpect(status().isOk());

    // 2) List all flashcards and grab the 'front' value
    MvcResult listRes =
        mockMvc
            .perform(get("/flashcards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    FlashcardReviewDto[] all =
        mapper.readValue(listRes.getResponse().getContentAsString(), FlashcardReviewDto[].class);
    String front = all[0].getFront();

    // 3) Delete by front instead of UUID
    mockMvc
        .perform(delete("/flashcards").with(csrf()).param("front", front))
        .andExpect(status().isNoContent());

    // 4) Verify it no longer appears in the list
    MvcResult after =
        mockMvc
            .perform(get("/flashcards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    FlashcardReviewDto[] remaining =
        mapper.readValue(after.getResponse().getContentAsString(), FlashcardReviewDto[].class);
    assertThat(Arrays.stream(remaining).map(FlashcardReviewDto::getFront)).doesNotContain(front);
  }
}
