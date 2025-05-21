package com.andremunay.hobbyhub.spanish.app;

import java.time.LocalDate;
import java.util.List;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;

public class Sm2ReviewScheduler implements ReviewScheduler {

    @Override
    public List<Flashcard> getDue(LocalDate today) {
        // This will pull from DB in future
        return List.of();
    }

        /**
     * Updates flashcard review state using the SM-2 algorithm.
     * 
     * @param card  the flashcard to update
     * @param grade the recall quality (0–5)
     * @param today the date of review
     * @return the updated flashcard
     */
    public Flashcard review(Flashcard card, int grade, LocalDate today) {
        int repetition = card.getRepetition();
        double ef = card.getEasinessFactor();
        int interval = 0;

        if (grade < 3) {
            repetition = 0;
            interval = 1;
        } else {
            repetition++;
            interval = (int) (switch (repetition) {
                case 1 -> 1;
                case 2 -> 6;
                default -> Math.round(interval * ef);
            });

            //EF update formula
            ef = ef + (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02));
            if (ef < 1.3) ef = 1.3;
        }

        card.setRepetition(repetition);
        card.setEasinessFactor(ef);
        card.setNextReviewOn(today.plusDays(interval));
        return card;
    }

}
