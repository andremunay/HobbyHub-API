package com.andremunay.hobbyhub.spanish.app;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import java.util.List;

public interface ReviewScheduler {
  List<Flashcard> getDue(LocalDate today);
}
