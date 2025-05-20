package com.andremunay.hobbyhub.spanish.infra;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {
  List<Flashcard> findByNextReviewOnBefore(LocalDate dueDate);
}
