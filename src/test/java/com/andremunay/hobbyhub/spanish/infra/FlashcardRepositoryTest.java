package com.andremunay.hobbyhub.spanish.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.andremunay.hobbyhub.spanish.domain.Flashcard;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    excludeAutoConfiguration = {LiquibaseAutoConfiguration.class, FlywayAutoConfiguration.class})
@AutoConfigureTestDatabase
public class FlashcardRepositoryTest {

  @Autowired private FlashcardRepository repository;

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
