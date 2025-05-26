package com.andremunay.hobbyhub.spanish.app;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import com.andremunay.hobbyhub.spanish.infra.FlashcardRepository;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FlashcardService {

  private final FlashcardRepository repository;
  private final ReviewScheduler scheduler;

  public FlashcardService(FlashcardRepository repository, ReviewScheduler scheduler) {
    this.repository = repository;
    this.scheduler = scheduler;
  }

  /** Create and save a new flashcard. */
  public void create(String front, String back) {
    Flashcard card = new Flashcard(UUID.randomUUID(), front, back);
    repository.save(card);
  }

  /** Retrieve all flashcards as DTOs. */
  public Collection<FlashcardReviewDto> getAll() {
    return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
  }

  /** Retrieve only flashcards due for review on or before the given date. */
  public List<FlashcardReviewDto> getDue(LocalDate today) {
    // 1) load due cards from DB
    List<Flashcard> dueCards = repository.findByNextReviewOnLessThanEqual(today);
    return dueCards.stream().map(this::toDto).collect(Collectors.toList());
  }

  private FlashcardReviewDto toDto(Flashcard card) {
    return new FlashcardReviewDto(
        card.getId(), card.getFront(), card.getBack(), card.getNextReviewOn());
  }

  public FlashcardReviewDto review(UUID id, int grade) {
    Flashcard card =
        repository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Flashcard not found: " + id));
    // apply SM-2 logic
    Flashcard updated = scheduler.review(card, grade, LocalDate.now());
    // persist
    repository.save(updated);
    // map to DTO and return
    return toDto(updated);
  }
}
