package com.andremunay.hobbyhub.spanish.app;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * An implementation of the SM-2 spaced repetition algorithm used in SuperMemo.
 *
 * <p>This algorithm dynamically adjusts the review interval and easiness factor (EF) based on how
 * well the user recalls the flashcard. It is designed to optimize retention by scheduling
 * increasingly spaced reviews for well-remembered items.
 *
 * <p>Reference: https://www.supermemo.com/en/archives1990-2015/english/ol/sm2
 */
@Service
public class Sm2ReviewScheduler implements ReviewScheduler {

  /**
   * Applies the SM-2 review algorithm to reschedule a flashcard.
   *
   * @param card the flashcard being reviewed
   * @param grade the user's score (0–5) indicating recall quality
   * @param today the date the review took place
   * @return the updated flashcard with new interval, repetition count, and easiness factor
   */
  @Override
  public Flashcard review(Flashcard card, int grade, LocalDate today) {
    int prevRepetition = card.getRepetition();
    double ef = card.getEasinessFactor();
    int prevInterval = card.getInterval();

    int repetition;
    int interval;
    if (grade < 3) {
      // Poor recall: reset repetition, schedule soon
      repetition = 0;
      interval = 1;
    } else {
      // Good recall: increase repetition count and compute next interval
      repetition = prevRepetition + 1;
      interval =
          (int)
              (switch (repetition) {
                case 1 -> 1;
                case 2 -> 6;
                default -> Math.round(prevInterval * ef);
              });
    }

    // Adjust easiness factor based on recall performance
    ef = ef + (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02));
    if (ef < 1.3) {
      ef = 1.3;
    }

    // Persist updated scheduling metrics to the flashcard
    card.setRepetition(repetition);
    card.setInterval(interval);
    card.setEasinessFactor(ef);
    card.setNextReviewOn(today.plusDays(interval));

    return card;
  }
}
