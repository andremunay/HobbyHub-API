package com.andremunay.hobbyhub.spanish.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

@AutoConfigureMockMvc
@Import(com.andremunay.hobbyhub.TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Transactional
public class SpanishIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

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
}
