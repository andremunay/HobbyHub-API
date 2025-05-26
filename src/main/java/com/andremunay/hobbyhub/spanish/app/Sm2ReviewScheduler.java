package com.andremunay.hobbyhub.spanish.app;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class Sm2ReviewScheduler implements ReviewScheduler {

  /**
   * Updates flashcard review state using the SM-2 spaced repetition algorithm.
   *
   * @param card the flashcard to update
   * @param grade the recall quality (0–5)
   * @param today the date of review
   * @return the updated flashcard
   */
  @Override
  public Flashcard review(Flashcard card, int grade, LocalDate today) {
    int prevRepetition = card.getRepetition();
    double ef = card.getEasinessFactor();
    int prevInterval = card.getInterval();

    // 1) Determine new repetition and base interval
    int repetition;
    int interval;
    if (grade < 3) {
      repetition = 0;
      interval = 1;
    } else {
      repetition = prevRepetition + 1;
      interval =
          (int)
              (switch (repetition) {
                case 1 -> 1;
                case 2 -> 6;
                default -> Math.round(prevInterval * ef);
              });
    }

    // 2) Update ease factor and clamp to minimum of 1.3
    ef = ef + (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02));
    if (ef < 1.3) {
      ef = 1.3;
    }

    // 3) Apply all updates back to the flashcard
    card.setRepetition(repetition);
    card.setInterval(interval);
    card.setEasinessFactor(ef);
    card.setNextReviewOn(today.plusDays(interval));

    return card;
  }
}
