package com.andremunay.hobbyhub.spanish.app;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;

public interface ReviewScheduler {
  Flashcard review(Flashcard card, int grade, LocalDate today);
}
