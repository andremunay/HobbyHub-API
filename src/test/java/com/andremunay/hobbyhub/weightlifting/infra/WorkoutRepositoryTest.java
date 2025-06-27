package com.andremunay.hobbyhub.weightlifting.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSetId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link WorkoutRepository} using a real PostgreSQL container.
 *
 * <p>Validates: - Custom query for fetching sets by exercise ID in descending date order -
 * Fetch-join behavior for loading workouts with sets in one query
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(com.andremunay.hobbyhub.TestcontainersConfiguration.class)
@Testcontainers
class WorkoutRepositoryTest {

  @Autowired private WorkoutRepository workoutRepository;
  @Autowired private TestEntityManager entityManager;

  @BeforeEach
  void clearDatabase() {
    workoutRepository.deleteAll();
  }

  /**
   * Ensures that {@code findSetsByExerciseId} returns workout sets in descending workout date
   * order.
   */
  @Test
  @DisplayName("findSetsByExerciseId returns WorkoutSets ordered by workout date descending")
  void findSetsByExerciseId() {
    Exercise exercise = new Exercise(UUID.randomUUID(), "test", "test");
    entityManager.persist(exercise);

    Workout older = new Workout(UUID.randomUUID(), LocalDate.now().minusDays(5));
    WorkoutSetId id1 = new WorkoutSetId(older.getId(), 0);
    WorkoutSet set1 = new WorkoutSet(id1, exercise, new BigDecimal("100.0"), 5);
    older.addSet(set1);
    workoutRepository.save(older);

    Workout newer = new Workout(UUID.randomUUID(), LocalDate.now());
    WorkoutSetId id2 = new WorkoutSetId(newer.getId(), 0);
    WorkoutSet set2 = new WorkoutSet(id2, exercise, new BigDecimal("110.0"), 5);
    newer.addSet(set2);
    workoutRepository.save(newer);

    List<WorkoutSet> results =
        workoutRepository.findSetsByExerciseId(exercise.getId(), PageRequest.of(0, 2));

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getWorkout().getId()).isEqualTo(newer.getId());
    assertThat(results.get(1).getWorkout().getId()).isEqualTo(older.getId());
  }

  /**
   * Verifies that {@code findAllWithSets} performs an eager fetch of workout sets. This avoids N+1
   * query issues by retrieving sets along with workouts.
   */
  @Test
  @DisplayName("findAllWithSets fetches workouts with their sets eagerly")
  void findAllWithSets() {
    Exercise exercise = new Exercise(UUID.randomUUID(), "test", "test");
    entityManager.persist(exercise);

    Workout workout = new Workout(UUID.randomUUID(), LocalDate.now());
    WorkoutSetId setId = new WorkoutSetId(workout.getId(), 0);
    WorkoutSet set = new WorkoutSet(setId, exercise, new BigDecimal("80.0"), 10);
    workout.addSet(set);
    workoutRepository.save(workout);

    List<Workout> all = workoutRepository.findAllWithSets();

    assertThat(all).hasSize(1);
    assertThat(all.get(0).getSets())
        .hasSize(1)
        .first()
        .extracting(WorkoutSet::getExercise)
        .extracting(Exercise::getId)
        .isEqualTo(exercise.getId());
  }
}
