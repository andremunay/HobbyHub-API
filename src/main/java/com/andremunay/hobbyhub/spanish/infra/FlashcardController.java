package com.andremunay.hobbyhub.spanish.infra;

import com.andremunay.hobbyhub.spanish.app.ReviewScheduler;
import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardCreateRequest;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/flashcards")
public class FlashcardController {
  private final FlashcardRepository repository;
  private final ReviewScheduler scheduler;

  public FlashcardController(FlashcardRepository repository, ReviewScheduler scheduler) {
    this.repository = repository;
    this.scheduler = scheduler;
  }

  @PostMapping
  public ResponseEntity<Void> create(@Valid @RequestBody FlashcardCreateRequest req) {
    Flashcard card = new Flashcard(UUID.randomUUID(), req.getFront(), req.getBack());
    repository.save(card);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  public ResponseEntity<List<FlashcardReviewDto>> getAll() {
    return ResponseEntity.ok(
        repository.findAll().stream().map(this::toDto).collect(Collectors.toList()));
  }

  @GetMapping("/review")
  public ResponseEntity<List<FlashcardReviewDto>> getDue(@RequestParam boolean due) {
    List<Flashcard> cards =
        due ? repository.findByNextReviewOnBefore(LocalDate.now()) : repository.findAll();

    return ResponseEntity.ok(cards.stream().map(this::toDto).collect(Collectors.toList()));
  }

  private FlashcardReviewDto toDto(Flashcard card) {
    FlashcardReviewDto dto = new FlashcardReviewDto();
    dto.setId(card.getId());
    dto.setFront(card.getFront());
    dto.setBack(card.getBack());
    dto.setNextReviewOn(card.getNextReviewOn());
    return dto;
  }
}
