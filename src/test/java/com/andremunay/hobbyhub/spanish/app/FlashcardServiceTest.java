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

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

  @Mock private FlashcardRepository repository;

  @Mock private ReviewScheduler scheduler;

  @InjectMocks private FlashcardService flashcardService;

  @Captor private ArgumentCaptor<Flashcard> flashcardCaptor;

  @Test
  void createShouldSaveNewFlashcard() {
    // Arrange
    String front = "hola";
    String back = "hello";

    // Act
    flashcardService.create(front, back);

    // Assert
    verify(repository).save(flashcardCaptor.capture());
    Flashcard saved = flashcardCaptor.getValue();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getFront()).isEqualTo(front);
    assertThat(saved.getBack()).isEqualTo(back);
  }

  @Test
  void getAllShouldReturnMappedDtos() {
    // Arrange
    UUID id = UUID.randomUUID();
    LocalDate next = LocalDate.of(2025, Month.MAY, 26);
    Flashcard card = new Flashcard(id, "f", "b");
    card.setNextReviewOn(next);
    when(repository.findAll()).thenReturn(List.of(card));

    // Act
    Collection<FlashcardReviewDto> dtos = flashcardService.getAll();

    // Assert
    assertThat(dtos).hasSize(1);
    FlashcardReviewDto dto = dtos.iterator().next();
    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getFront()).isEqualTo("f");
    assertThat(dto.getBack()).isEqualTo("b");
    assertThat(dto.getNextReviewOn()).isEqualTo(next);
  }

  @Test
  void getDueShouldReturnOnlyDueMappedDtos() {
    // Arrange
    UUID id = UUID.randomUUID();
    LocalDate today = LocalDate.of(2025, Month.MAY, 26);
    Flashcard card = new Flashcard(id, "x", "y");
    card.setNextReviewOn(today.minusDays(1));
    when(repository.findByNextReviewOnLessThanEqual(today)).thenReturn(List.of(card));

    // Act
    List<FlashcardReviewDto> dtos = flashcardService.getDue(today);

    // Assert
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

  @Test
  void reviewShouldThrowWhenNotFound() {
    // Arrange
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> flashcardService.review(id, 3))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void reviewShouldApplySchedulerAndSaveAndReturnDto() {
    // Arrange
    UUID id = UUID.randomUUID();
    LocalDate today = LocalDate.of(2025, Month.MAY, 26);
    Flashcard original = new Flashcard(id, "a", "b");
    original.setRepetition(1);
    original.setEasinessFactor(2.0);
    original.setInterval(1);
    original.setNextReviewOn(today);

    Flashcard updated = new Flashcard(id, "a", "b");
    updated.setRepetition(2);
    updated.setEasinessFactor(2.1);
    updated.setInterval(6);
    updated.setNextReviewOn(today.plusDays(6));

    when(repository.findById(id)).thenReturn(Optional.of(original));
    when(scheduler.review(eq(original), eq(5), any(LocalDate.class))).thenReturn(updated);

    // Act
    FlashcardReviewDto dto = flashcardService.review(id, 5);

    // Assert
    verify(repository).save(updated);
    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getFront()).isEqualTo("a");
    assertThat(dto.getBack()).isEqualTo("b");
    assertThat(dto.getNextReviewOn()).isEqualTo(today.plusDays(6));
  }
}
