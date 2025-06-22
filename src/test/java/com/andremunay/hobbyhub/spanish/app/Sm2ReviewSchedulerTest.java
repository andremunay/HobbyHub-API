package com.andremunay.hobbyhub.spanish.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link Sm2ReviewScheduler}, verifying SM-2 algorithm behavior.
 *
 * <p>Covers scheduling logic for first and subsequent reviews, easiness factor updates, repetition
 * resets, and lower-bound enforcement.
 */
class Sm2ReviewSchedulerTest {

  private Sm2ReviewScheduler scheduler;
  private LocalDate today;

  @BeforeEach
  void setUp() {
    scheduler = new Sm2ReviewScheduler();
    today = LocalDate.of(2025, 5, 20);
  }

  // Helper method to build a flashcard with controlled review state
  private Flashcard createCard(int repetition, double ef, int interval) {
    Flashcard card = new Flashcard(UUID.randomUUID(), "hola", "hello");
    card.setRepetition(repetition);
    card.setEasinessFactor(ef);
    card.setInterval(interval);
    return card;
  }

  /** Verifies that the first review schedules correctly: repetition = 1, interval = 1. */
  @Test
  void shouldScheduleFirstReviewCorrectly() {
    Flashcard card = createCard(0, 2.5, 0);
    Flashcard reviewed = scheduler.review(card, 5, today);

    assertEquals(1, reviewed.getRepetition());
    assertEquals(1, reviewed.getInterval());
    assertEquals(today.plusDays(1), reviewed.getNextReviewOn());
  }

  /** Verifies that the second review produces a hardcoded interval of 6 days. */
  @Test
  void shouldScheduleSecondReviewCorrectly() {
    Flashcard card = createCard(1, 2.5, 1);
    Flashcard reviewed = scheduler.review(card, 5, today);

    assertEquals(2, reviewed.getRepetition());
    assertEquals(6, reviewed.getInterval());
    assertEquals(today.plusDays(6), reviewed.getNextReviewOn());
  }

  /** Parameterized test: verifies interval scaling via easiness factor after third+ review. */
  @ParameterizedTest
  @CsvSource({"2.5, 5, 15", "2.0, 3, 12", "2.0, 4, 12"})
  void shouldUseEasinessFactorWhenReviewingWithGoodGrades(
      double ef, int grade, int expectedInterval) {
    Flashcard card = createCard(2, ef, 6);
    Flashcard reviewed = scheduler.review(card, grade, today);

    assertEquals(3, reviewed.getRepetition());
    assertEquals(expectedInterval, reviewed.getInterval());
    assertEquals(today.plusDays(expectedInterval), reviewed.getNextReviewOn());
  }

  /** Verifies that a low-quality review resets repetition and interval. */
  @Test
  void shouldResetIntervalAndRepetitionWhenQualityIsLow() {
    Flashcard card = createCard(3, 2.5, 10);
    Flashcard reviewed = scheduler.review(card, 2, today);

    assertEquals(0, reviewed.getRepetition());
    assertEquals(1, reviewed.getInterval());
    assertEquals(today.plusDays(1), reviewed.getNextReviewOn());
  }

  /** Verifies that easiness factor is clamped to a minimum of 1.3 after poor recall. */
  @Test
  void shouldClampEaseFactorMinimumAt1point3() {
    Flashcard card = createCard(2, 1.2, 6);
    Flashcard reviewed = scheduler.review(card, 0, today);

    assertTrue(reviewed.getEasinessFactor() >= 1.3);
  }

  /** Verifies that a perfect review increases the easiness factor. */
  @Test
  void shouldUpdateEaseFactorUpwardsWhenQualityIsHigh() {
    Flashcard card = createCard(1, 2.0, 1);
    Flashcard reviewed = scheduler.review(card, 5, today);

    assertTrue(reviewed.getEasinessFactor() > 2.0);
  }
}
