package com.andremunay.hobbyhub.spanish.infra;

import com.andremunay.hobbyhub.spanish.app.FlashcardService;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardDto;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardGradeDto;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing flashcards and their review lifecycle.
 *
 * <p>Exposes endpoints to create, review, retrieve, and delete flashcards, supporting spaced
 * repetition via the {@link FlashcardService}.
 */
@RestController
@RequestMapping("/flashcards")
@RequiredArgsConstructor
public class FlashcardController {
  private final FlashcardService flashcardService;

  /**
   * Retrieves all flashcards, regardless of review status.
   *
   * @return HTTP 200 with a list of flashcard DTOs
   */
  @GetMapping
  public ResponseEntity<List<FlashcardReviewDto>> getAll() {
    List<FlashcardReviewDto> dtos = List.copyOf(flashcardService.getAll());
    return ResponseEntity.ok(dtos);
  }

  /**
   * Retrieves either all or only due flashcards based on query param.
   *
   * @param due if true, only returns flashcards due for review as of today
   * @return HTTP 200 with a filtered list of flashcard DTOs
   */
  @GetMapping("/review")
  public ResponseEntity<List<FlashcardReviewDto>> getDue(@RequestParam boolean due) {
    List<FlashcardReviewDto> dtos =
        due
            ? List.copyOf(flashcardService.getDue(LocalDate.now()))
            : List.copyOf(flashcardService.getAll());
    return ResponseEntity.ok(dtos);
  }

  /**
   * Creates a new flashcard with front and back text.
   *
   * @param req validated DTO with flashcard content
   * @return HTTP 200 on successful creation
   */
  @PostMapping
  public ResponseEntity<Void> create(@Valid @RequestBody FlashcardDto req) {
    flashcardService.create(req.getFront(), req.getBack());
    return ResponseEntity.ok().build();
  }

  /**
   * Submits a review result for a given flashcard and returns the updated scheduling info.
   *
   * @param req validated review score DTO
   * @return HTTP 200 with updated flashcard DTO
   */
  @PostMapping("/review")
  public ResponseEntity<FlashcardReviewDto> review(@Valid @RequestBody FlashcardGradeDto req) {
    FlashcardReviewDto updated = flashcardService.review(req.getFront(), req.getGrade());
    return ResponseEntity.ok(updated);
  }

  /**
   * Deletes a flashcard by ID.
   *
   * @param front the name of the flashcard to delete
   * @return HTTP 204 if the deletion is successful
   */
  @DeleteMapping
  public ResponseEntity<Void> deleteCard(@RequestParam("front") String front) {
    flashcardService.delete(front);
    return ResponseEntity.noContent().build();
  }
}
