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

  @Test
  @DisplayName("findSetsByExerciseId returns WorkoutSets ordered by workout date descending")
  void findSetsByExerciseId() {
    // given: an exercise and two workouts on different dates
    Exercise exercise = new Exercise(UUID.randomUUID(), "Squat", "Legs");
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

    // when: fetching last 2 sets for the exercise
    List<WorkoutSet> results =
        workoutRepository.findSetsByExerciseId(exercise.getId(), PageRequest.of(0, 2));

    // then: ordered by workout.performedOn descending
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getWorkout().getId()).isEqualTo(newer.getId());
    assertThat(results.get(1).getWorkout().getId()).isEqualTo(older.getId());
  }

  @Test
  @DisplayName("findAllWithSets fetches workouts with their sets eagerly")
  void findAllWithSets() {
    // given: a workout with one set
    Exercise exercise = new Exercise(UUID.randomUUID(), "Bench Press", "Chest");
    entityManager.persist(exercise);

    Workout workout = new Workout(UUID.randomUUID(), LocalDate.now());
    WorkoutSetId setId = new WorkoutSetId(workout.getId(), 0);
    WorkoutSet set = new WorkoutSet(setId, exercise, new BigDecimal("80.0"), 10);
    workout.addSet(set);
    workoutRepository.save(workout);

    // when: loading all workouts with sets
    List<Workout> all = workoutRepository.findAllWithSets();

    // then: each workout contains its sets
    assertThat(all).hasSize(1);
    assertThat(all.get(0).getSets())
        .hasSize(1)
        .first()
        .extracting(WorkoutSet::getExercise)
        .extracting(Exercise::getId)
        .isEqualTo(exercise.getId());
  }
}
