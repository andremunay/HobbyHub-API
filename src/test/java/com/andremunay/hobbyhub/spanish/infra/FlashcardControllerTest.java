package com.andremunay.hobbyhub.spanish.infra;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andremunay.hobbyhub.spanish.app.FlashcardService;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class FlashcardControllerTest {

  private MockMvc mvc;

  @Mock private FlashcardService flashcardService;

  @InjectMocks private FlashcardController controller;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void createAndGetAll() throws Exception {
    // Create request
    mvc.perform(
            post("/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"front\":\"hola\",\"back\":\"hello\"}"))
        .andExpect(status().isOk());

    // Stub service.getAll()
    var id = UUID.randomUUID();
    var nextReview = LocalDate.now();
    FlashcardReviewDto dto = new FlashcardReviewDto(id, "hola", "hello", nextReview);
    BDDMockito.given(flashcardService.getAll()).willReturn(List.of(dto));

    // Execute GET /flashcards
    mvc.perform(get("/flashcards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].front").value("hola"));
  }

  @Test
  void getDue() throws Exception {
    // Stub service.getDue(...)
    var id = UUID.randomUUID();
    var nextReview = LocalDate.now().minusDays(1);
    FlashcardReviewDto dto = new FlashcardReviewDto(id, "A", "B", nextReview);
    BDDMockito.given(flashcardService.getDue(Mockito.any(LocalDate.class)))
        .willReturn(List.of(dto));

    // Execute GET /flashcards/review?due=true
    mvc.perform(get("/flashcards/review").param("due", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].back").value("B"));
  }

  @Test
  void reviewEndpointShouldApplyGradeAndReturnUpdatedDto() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    int grade = 4;
    LocalDate nextReview = LocalDate.now().plusDays(6);
    FlashcardReviewDto updatedDto = new FlashcardReviewDto(id, "hola", "hello", nextReview);

    BDDMockito.given(flashcardService.review(id, grade)).willReturn(updatedDto);

    String payload = String.format("{\"grade\":%d}", grade);

    // Act & Assert
    mvc.perform(
            post("/flashcards/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nextReviewOn").value(nextReview.toString()));
  }
}
