package com.andremunay.hobbyhub.spanish.app;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;

/**
 * Strategy interface for determining how flashcards are rescheduled after a review.
 *
 * <p>Implementations can define custom spaced repetition algorithms such as SM-2 or others.
 */
public interface ReviewScheduler {
  /**
   * Applies a review result to the given flashcard and calculates its next review date.
   *
   * @param card the flashcard being reviewed
   * @param grade the review score (e.g., 0–5 where higher is better recall)
   * @param today the current date of the review
   * @return a new or updated Flashcard instance with the revised scheduling
   */
  Flashcard review(Flashcard card, int grade, LocalDate today);
}
