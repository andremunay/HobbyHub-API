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
import java.util.UUID;
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
    String flashcardJson = "{\"front\":\"test\",\"back\":\"test\"}";
    mockMvc
        .perform(
            post("/flashcards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(flashcardJson))
        .andExpect(status().isOk());

    MvcResult result =
        mockMvc
            .perform(get("/flashcards/review?due=true").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

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
    String create = "{\"front\":\"uno\",\"back\":\"one\"}";
    mockMvc
        .perform(
            post("/flashcards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(create))
        .andExpect(status().isOk());

    MvcResult listRes =
        mockMvc
            .perform(get("/flashcards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    FlashcardReviewDto[] all =
        mapper.readValue(listRes.getResponse().getContentAsString(), FlashcardReviewDto[].class);
    UUID id = all[0].getId();

    String gradeJson = "{\"grade\":5}";
    MvcResult revRes =
        mockMvc
            .perform(
                post("/flashcards/" + id + "/review")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(gradeJson))
            .andExpect(status().isOk())
            .andReturn();

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
    String create = "{\"front\":\"dos\",\"back\":\"two\"}";
    mockMvc
        .perform(
            post("/flashcards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(create))
        .andExpect(status().isOk());

    MvcResult listRes =
        mockMvc
            .perform(get("/flashcards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    FlashcardReviewDto[] all =
        mapper.readValue(listRes.getResponse().getContentAsString(), FlashcardReviewDto[].class);
    UUID id = all[0].getId();

    mockMvc.perform(delete("/flashcards/" + id).with(csrf())).andExpect(status().isNoContent());

    MvcResult after =
        mockMvc
            .perform(get("/flashcards").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    FlashcardReviewDto[] remaining =
        mapper.readValue(after.getResponse().getContentAsString(), FlashcardReviewDto[].class);
    assertThat(Arrays.stream(remaining).map(FlashcardReviewDto::getId)).doesNotContain(id);
  }
}
