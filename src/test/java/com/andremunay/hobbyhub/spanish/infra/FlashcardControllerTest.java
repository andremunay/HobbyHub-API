package com.andremunay.hobbyhub.spanish.infra;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.junit.jupiter.api.DisplayName;
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

/**
 * Unit tests for {@link FlashcardController}, validating its REST behavior, including response
 * structure, status codes, service delegation, and parameter handling.
 */
@ExtendWith(MockitoExtension.class)
class FlashcardControllerTest {

  private MockMvc mvc;

  @Mock private FlashcardService flashcardService;

  @InjectMocks private FlashcardController controller;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  /** Verifies that creating a flashcard returns HTTP 200 and that getAll returns expected JSON. */
  @Test
  void createAndGetAll() throws Exception {
    mvc.perform(
            post("/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"front\":\"hola\",\"back\":\"hello\"}"))
        .andExpect(status().isOk());

    var id = UUID.randomUUID();
    var nextReview = LocalDate.now();
    FlashcardReviewDto dto = new FlashcardReviewDto(id, "hola", "hello", nextReview);
    BDDMockito.given(flashcardService.getAll()).willReturn(List.of(dto));

    mvc.perform(get("/flashcards"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].front").value("hola"));
  }

  /** Verifies that the controller filters flashcards due for review and returns them correctly. */
  @Test
  void getDue() throws Exception {
    var id = UUID.randomUUID();
    var nextReview = LocalDate.now().minusDays(1);
    FlashcardReviewDto dto = new FlashcardReviewDto(id, "A", "B", nextReview);
    BDDMockito.given(flashcardService.getDue(Mockito.any(LocalDate.class)))
        .willReturn(List.of(dto));

    mvc.perform(get("/flashcards/review").param("due", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].back").value("B"));
  }

  /** Verifies that the review endpoint accepts a grade and returns an updated DTO. */
  @Test
  void reviewEndpointShouldApplyGradeAndReturnUpdatedDto() throws Exception {
    UUID id = UUID.randomUUID();
    int grade = 4;
    LocalDate nextReview = LocalDate.now().plusDays(6);
    FlashcardReviewDto updatedDto = new FlashcardReviewDto(id, "hola", "hello", nextReview);

    BDDMockito.given(flashcardService.review(id, grade)).willReturn(updatedDto);

    String payload = String.format("{\"grade\":%d}", grade);

    mvc.perform(
            post("/flashcards/{id}/review", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nextReviewOn").value(nextReview.toString()));
  }

  /** Verifies that a flashcard can be deleted by ID. */
  @Test
  void deleteCard() throws Exception {
    UUID id = UUID.randomUUID();
    BDDMockito.willDoNothing().given(flashcardService).delete(id);

    mvc.perform(delete("/flashcards/{id}", id)).andExpect(status().isNoContent());

    Mockito.verify(flashcardService).delete(id);
  }

  /** Verifies that only due flashcards are returned when ?due=true is specified. */
  @Test
  @DisplayName("GET /flashcards/review?due=true → returns only due cards")
  void getDue_whenTrue_returnsOnlyDue() throws Exception {
    var dueCard =
        new FlashcardReviewDto(UUID.randomUUID(), "uno", "one", LocalDate.now().minusDays(1));
    when(flashcardService.getDue(LocalDate.now())).thenReturn(List.of(dueCard));

    mvc.perform(get("/flashcards/review").param("due", "true").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].front").value("uno"))
        .andExpect(jsonPath("$[0].id").value(dueCard.getId().toString()));

    verify(flashcardService).getDue(LocalDate.now());
    verifyNoMoreInteractions(flashcardService);
  }

  /** Verifies that all flashcards are returned when ?due=false is specified. */
  @Test
  @DisplayName("GET /flashcards/review?due=false → returns all cards")
  void getDue_whenFalse_returnsAll() throws Exception {
    var c1 = new FlashcardReviewDto(UUID.randomUUID(), "dos", "two", LocalDate.now());
    var c2 =
        new FlashcardReviewDto(UUID.randomUUID(), "tres", "three", LocalDate.now().plusDays(1));
    when(flashcardService.getAll()).thenReturn(List.of(c1, c2));

    mvc.perform(get("/flashcards/review").param("due", "false").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[?(@.front=='dos')]").exists())
        .andExpect(jsonPath("$[?(@.front=='tres')]").exists());

    verify(flashcardService).getAll();
    verifyNoMoreInteractions(flashcardService);
  }

  /** Asserts that omitting the 'due' query parameter results in a 400 Bad Request. */
  @Test
  @DisplayName("GET /flashcards/review → missing ‘due’ param yields 400 Bad Request")
  void getDue_missingParam_badRequest() throws Exception {
    mvc.perform(get("/flashcards/review")).andExpect(status().isBadRequest());
  }

  /** Asserts that a non-boolean 'due' parameter results in a 400 Bad Request. */
  @Test
  @DisplayName("GET /flashcards/review?due=notBoolean → invalid ‘due’ param yields 400")
  void getDue_invalidParam_badRequest() throws Exception {
    mvc.perform(get("/flashcards/review").param("due", "foo")).andExpect(status().isBadRequest());
  }
}
