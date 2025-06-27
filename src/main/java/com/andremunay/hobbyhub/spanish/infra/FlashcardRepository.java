package com.andremunay.hobbyhub.spanish.infra;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for accessing flashcard persistence operations.
 *
 * <p>Inherits standard CRUD methods from {@link JpaRepository} and includes a custom finder for
 * retrieving cards due for review.
 */
public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
  /**
   * Finds flashcards whose next review date is on or before the specified due date.
   *
   * @param dueDate the latest review date to include (inclusive)
   * @return list of flashcards scheduled for review by that date
   */
  List<Flashcard> findByNextReviewOnLessThanEqual(LocalDate dueDate);

  Optional<Flashcard> findByFrontIgnoreCase(String front);
}
