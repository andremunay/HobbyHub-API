package com.andremunay.hobbyhub.spanish.app;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import com.andremunay.hobbyhub.spanish.infra.FlashcardRepository;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service for managing flashcards and their review schedule.
 *
 * <p>Provides CRUD operations and spaced repetition logic integration via {@link ReviewScheduler}.
 */
@Service
@RequiredArgsConstructor
public class FlashcardService {

  private final FlashcardRepository repository;
  private final ReviewScheduler scheduler;

  /**
   * Creates a new flashcard with the given front and back content.
   *
   * @param front the prompt side of the flashcard
   * @param back the answer or explanation side
   */
  @Transactional
  public void create(String front, String back) {
    Flashcard card = new Flashcard(UUID.randomUUID(), front, back);
    repository.save(card);
  }

  /**
   * Retrieves all flashcards in the system, regardless of review status.
   *
   * @return a collection of flashcards in review-ready DTO format
   */
  @Transactional
  public Collection<FlashcardReviewDto> getAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  /**
   * Retrieves flashcards that are due for review on or before the specified date.
   *
   * @param today the cutoff date for due reviews
   * @return a list of flashcard DTOs scheduled for review
   */
  @Transactional
  public List<FlashcardReviewDto> getDue(LocalDate today) {
    List<Flashcard> dueCards = repository.findByNextReviewOnLessThanEqual(today);
    return dueCards.stream().map(this::toDto).toList();
  }

  /**
   * Records a user's review result for a given flashcard and reschedules it accordingly.
   *
   * @param front the flashcard's unique identifier
   * @param grade the user's review score (e.g. 0–5 for SM2 algorithms)
   * @return the updated flashcard in DTO format
   * @throws EntityNotFoundException if the flashcard does not exist
   */
  @Transactional
  public FlashcardReviewDto review(String front, int grade) {
    // 1) normalize & lookup
    Flashcard card =
        repository
            .findByFrontIgnoreCase(front)
            .orElseThrow(
                () -> new EntityNotFoundException("Flashcard not found: \"" + front + "\""));

    // 2) perform SM-2 review and save
    Flashcard updated = scheduler.review(card, grade, LocalDate.now());
    repository.save(updated);

    // 3) map back to DTO
    return toDto(updated);
  }

  /**
   * Deletes a flashcard by its ID, if it exists.
   *
   * @param id the flashcard identifier
   * @throws EntityNotFoundException if the flashcard is not found
   */
  @Transactional
  public void delete(String front) {
    Flashcard card =
        repository
            .findByFrontIgnoreCase(front)
            .orElseThrow(
                () -> new EntityNotFoundException("Flashcard not found: \"" + front + "\""));

    // perform the delete
    repository.delete(card);
  }

  // Converts a Flashcard entity into a DTO for read operations
  private FlashcardReviewDto toDto(Flashcard card) {
    return new FlashcardReviewDto(
        card.getId(), card.getFront(), card.getBack(), card.getNextReviewOn());
  }
}
