package com.andremunay.hobbyhub.spanish.infra;

import com.andremunay.hobbyhub.spanish.app.FlashcardService;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardCreateRequest;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardGradeRequest;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flashcards")
public class FlashcardController {
  private final FlashcardService flashcardService;

  public FlashcardController(FlashcardService flashcardService) {
    this.flashcardService = flashcardService;
  }

  @PostMapping
  public ResponseEntity<Void> create(@Valid @RequestBody FlashcardCreateRequest req) {
    flashcardService.create(req.getFront(), req.getBack());
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<List<FlashcardReviewDto>> getAll() {
    List<FlashcardReviewDto> dtos = List.copyOf(flashcardService.getAll());
    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/review")
  public ResponseEntity<List<FlashcardReviewDto>> getDue(@RequestParam boolean due) {
    List<FlashcardReviewDto> dtos =
        due
            ? List.copyOf(flashcardService.getDue(LocalDate.now()))
            : List.copyOf(flashcardService.getAll());
    return ResponseEntity.ok(dtos);
  }

  @PostMapping("/{id}/review")
  public ResponseEntity<FlashcardReviewDto> review(
      @PathVariable UUID id, @Valid @RequestBody FlashcardGradeRequest req) {
    FlashcardReviewDto updated = flashcardService.review(id, req.getGrade());
    return ResponseEntity.ok(updated);
  }
}
