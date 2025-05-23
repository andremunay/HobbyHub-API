package com.andremunay.hobbyhub.spanish.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Sm2ReviewSchedulerTest {

  private Sm2ReviewScheduler scheduler;
  private LocalDate today;

  @BeforeEach
  void setUp() {
    scheduler = new Sm2ReviewScheduler();
    today = LocalDate.of(2025, 5, 20);
  }

  private Flashcard createCard(int repetition, double ef, int interval) {
    Flashcard card = new Flashcard(UUID.randomUUID(), "hola", "hello");
    card.setRepetition(repetition);
    card.setEasinessFactor(ef);
    card.setInterval(interval);
    return card;
  }

  @Test
  void shouldScheduleFirstReviewCorrectly() {
    Flashcard card = createCard(0, 2.5, 0);
    Flashcard reviewed = scheduler.review(card, 5, today);

    assertEquals(1, reviewed.getRepetition());
    assertEquals(1, reviewed.getInterval());
    assertEquals(today.plusDays(1), reviewed.getNextReviewOn());
  }

  @Test
  void shouldScheduleSecondReviewCorrectly() {
    Flashcard card = createCard(1, 2.5, 1);
    Flashcard reviewed = scheduler.review(card, 5, today);

    assertEquals(2, reviewed.getRepetition());
    assertEquals(6, reviewed.getInterval());
    assertEquals(today.plusDays(6), reviewed.getNextReviewOn());
  }

  @Test
  void shouldIncreaseIntervalUsingEFWhenRepetitionIs3Plus() {
    Flashcard card = createCard(2, 2.5, 6);
    Flashcard reviewed = scheduler.review(card, 5, today);

    assertEquals(3, reviewed.getRepetition());
    int expectedInterval = (int) Math.round(6 * 2.5);
    assertEquals(expectedInterval, reviewed.getInterval());
    assertEquals(today.plusDays(expectedInterval), reviewed.getNextReviewOn());
  }

  @Test
  void shouldHandleGradeOfThreeAsCorrect() {
    Flashcard card = createCard(2, 2.0, 6);
    Flashcard reviewed = scheduler.review(card, 3, today);

    assertEquals(3, reviewed.getRepetition());
    int expectedInterval = (int) Math.round(6 * 2.0);
    assertEquals(expectedInterval, reviewed.getInterval());
    assertEquals(today.plusDays(expectedInterval), reviewed.getNextReviewOn());
  }

  @Test
  void shouldHandleGradeOfFourAsCorrect() {
    Flashcard card = createCard(2, 2.0, 6);
    Flashcard reviewed = scheduler.review(card, 4, today);

    assertEquals(3, reviewed.getRepetition());
    int expectedInterval = (int) Math.round(6 * 2.0);
    assertEquals(expectedInterval, reviewed.getInterval());
    assertEquals(today.plusDays(expectedInterval), reviewed.getNextReviewOn());
  }

  @Test
  void shouldResetIntervalAndRepetitionWhenQualityIsLow() {
    Flashcard card = createCard(3, 2.5, 10);
    Flashcard reviewed = scheduler.review(card, 2, today);

    assertEquals(0, reviewed.getRepetition());
    assertEquals(1, reviewed.getInterval());
    assertEquals(today.plusDays(1), reviewed.getNextReviewOn());
  }

  @Test
  void shouldClampEaseFactorMinimumAt1point3() {
    Flashcard card = createCard(2, 1.2, 6);
    Flashcard reviewed = scheduler.review(card, 0, today);

    assertTrue(reviewed.getEasinessFactor() >= 1.3);
  }

  @Test
  void shouldUpdateEaseFactorUpwardsWhenQualityIsHigh() {
    Flashcard card = createCard(1, 2.0, 1);
    Flashcard reviewed = scheduler.review(card, 5, today);

    assertTrue(reviewed.getEasinessFactor() > 2.0);
  }
}
