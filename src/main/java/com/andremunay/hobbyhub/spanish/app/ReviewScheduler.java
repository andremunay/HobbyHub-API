package com.andremunay.hobbyhub.spanish.app;

import java.time.LocalDate;
import java.util.List;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;

public interface ReviewScheduler {
    List<Flashcard> getDue(LocalDate today);
}
