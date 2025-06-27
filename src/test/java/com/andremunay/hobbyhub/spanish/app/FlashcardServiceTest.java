package com.andremunay.hobbyhub.spanish.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import com.andremunay.hobbyhub.spanish.infra.FlashcardRepository;
import com.andremunay.hobbyhub.spanish.infra.dto.FlashcardReviewDto;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link FlashcardService}, verifying business logic and side effects such as
 * persistence, DTO mapping, review scheduling, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

  @Mock private FlashcardRepository repository;

  @Mock private ReviewScheduler scheduler;

  @InjectMocks private FlashcardService flashcardService;

  @Captor private ArgumentCaptor<Flashcard> flashcardCaptor;

  /** Verifies that creating a new flashcard results in a saved entity with correct fields. */
  @Test
  void createShouldSaveNewFlashcard() {
    String front = "hola";
    String back = "hello";

    flashcardService.create(front, back);

    verify(repository).save(flashcardCaptor.capture());
    Flashcard saved = flashcardCaptor.getValue();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getFront()).isEqualTo(front);
    assertThat(saved.getBack()).isEqualTo(back);
  }

  /** Ensures that all flashcards are fetched and mapped correctly to DTOs. */
  @Test
  void getAllShouldReturnMappedDtos() {
    UUID id = UUID.randomUUID();
    LocalDate next = LocalDate.of(2025, Month.MAY, 26);
    Flashcard card = new Flashcard(id, "f", "b");
    card.setNextReviewOn(next);
    when(repository.findAll()).thenReturn(List.of(card));

    Collection<FlashcardReviewDto> dtos = flashcardService.getAll();

    assertThat(dtos).hasSize(1);
    FlashcardReviewDto dto = dtos.iterator().next();
    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getFront()).isEqualTo("f");
    assertThat(dto.getBack()).isEqualTo("b");
    assertThat(dto.getNextReviewOn()).isEqualTo(next);
  }

  /** Verifies that only flashcards due on or before the provided date are returned. */
  @Test
  void getDueShouldReturnOnlyDueMappedDtos() {
    UUID id = UUID.randomUUID();
    LocalDate today = LocalDate.of(2025, Month.MAY, 26);
    Flashcard card = new Flashcard(id, "x", "y");
    card.setNextReviewOn(today.minusDays(1));
    when(repository.findByNextReviewOnLessThanEqual(today)).thenReturn(List.of(card));

    List<FlashcardReviewDto> dtos = flashcardService.getDue(today);

    assertThat(dtos)
        .singleElement()
        .satisfies(
            dto -> {
              assertThat(dto.getId()).isEqualTo(id);
              assertThat(dto.getFront()).isEqualTo("x");
              assertThat(dto.getBack()).isEqualTo("y");
              assertThat(dto.getNextReviewOn()).isEqualTo(today.minusDays(1));
            });
  }

  /** Ensures that an exception is thrown if a review is attempted on a non-existent flashcard. */
  @Test
  void reviewShouldThrowWhenNotFound() {
    String front = "nonexistent";
    when(repository.findByFrontIgnoreCase(front)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> flashcardService.review(front, 3))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(front);
  }

  /**
   * Verifies that the scheduler is applied during review, the card is updated and saved, and a
   * correctly mapped DTO is returned.
   */
  @Test
  void reviewShouldApplySchedulerAndSaveAndReturnDto() {
    String front = "hola";
    LocalDate today = LocalDate.of(2025, 5, 26);
    Flashcard original = new Flashcard(UUID.randomUUID(), front, "hello");
    original.setRepetition(1);
    original.setEasinessFactor(2.0);
    original.setInterval(1);
    original.setNextReviewOn(today);

    Flashcard updated = new Flashcard(original.getId(), front, "hello");
    updated.setRepetition(2);
    updated.setEasinessFactor(2.1);
    updated.setInterval(6);
    updated.setNextReviewOn(today.plusDays(6));

    when(repository.findByFrontIgnoreCase(front)).thenReturn(Optional.of(original));
    when(scheduler.review(eq(original), eq(5), any(LocalDate.class))).thenReturn(updated);

    FlashcardReviewDto dto = flashcardService.review(front, 5);

    verify(repository).save(updated);
    assertThat(dto.getId()).isEqualTo(original.getId());
    assertThat(dto.getFront()).isEqualTo(front);
    assertThat(dto.getBack()).isEqualTo("hello");
    assertThat(dto.getNextReviewOn()).isEqualTo(today.plusDays(6));
  }

  /** Asserts that an existing flashcard can be deleted by ID. */
  @Test
  void deleteShouldDeleteWhenExists() {
    String front = "hola";
    Flashcard card = new Flashcard(UUID.randomUUID(), front, "hello");
    when(repository.findByFrontIgnoreCase(front)).thenReturn(Optional.of(card));

    flashcardService.delete(front);

    verify(repository).delete(card);
  }

  /** Ensures an exception is thrown when attempting to delete a non-existent flashcard. */
  @Test
  void deleteShouldThrowWhenNotFound() {
    String front = "adios";
    when(repository.findByFrontIgnoreCase(front)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> flashcardService.delete(front))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(front);
  }
}
