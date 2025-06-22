package com.andremunay.hobbyhub.spanish.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link FlashcardRepository} using a real Postgres Testcontainer.
 *
 * <p>Validates that custom queries work as expected with actual JPA behavior and persistence
 * context.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(com.andremunay.hobbyhub.TestcontainersConfiguration.class)
@Testcontainers
class FlashcardRepositoryTest {

  @Autowired private FlashcardRepository repository;

  @BeforeEach
  void clearDatabase() {
    repository.deleteAll();
  }

  /** Ensures that only flashcards due on or before today are returned from the custom query. */
  @Test
  @DisplayName("findByNextReviewOnBefore returns only due cards")
  void findsDueFlashcards() {
    Flashcard upcoming = new Flashcard(UUID.randomUUID(), "A", "B");
    upcoming.setNextReviewOn(LocalDate.now().plusDays(1));
    Flashcard due = new Flashcard(UUID.randomUUID(), "C", "D");
    due.setNextReviewOn(LocalDate.now().minusDays(1));
    repository.saveAll(List.of(upcoming, due));

    var results = repository.findByNextReviewOnLessThanEqual(LocalDate.now());

    assertThat(results).hasSize(1).first().extracting(Flashcard::getId).isEqualTo(due.getId());
  }
}
