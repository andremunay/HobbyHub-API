package com.andremunay.hobbyhub.weightlifting.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSetId;
import com.andremunay.hobbyhub.weightlifting.infra.ExerciseRepository;
import com.andremunay.hobbyhub.weightlifting.infra.WorkoutRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

public class WeightliftingServiceTest {

  private WorkoutRepository repo;
  private WeightliftingService service;
  private final UUID exerciseId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    WorkoutRepository workoutRepo = Mockito.mock(WorkoutRepository.class);
    ExerciseRepository exerciseRepo = Mockito.mock(ExerciseRepository.class);
    service = new WeightliftingService(workoutRepo, exerciseRepo, new EpleyOneRepMaxStrategy());
  }

  @Test
  void computeOverloadTrend_returnsPositiveSlope_forStrictlyIncreasingWeights() {
    Exercise e1 = new Exercise(exerciseId, "Bench Press", "Push");

    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 2));
    Workout w3 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 3));

    WorkoutSet s1 = new WorkoutSet(new WorkoutSetId(w1.getId(), 1), e1, BigDecimal.valueOf(50), 5);
    s1.setWorkout(w1);
    WorkoutSet s2 = new WorkoutSet(new WorkoutSetId(w2.getId(), 1), e1, BigDecimal.valueOf(60), 5);
    s2.setWorkout(w2);
    WorkoutSet s3 = new WorkoutSet(new WorkoutSetId(w3.getId(), 1), e1, BigDecimal.valueOf(0), 5);

    List<WorkoutSet> syntheticSets = List.of(s1, s2, s3);

    Mockito.when(repo.findSetsByExerciseId(Mockito.eq(exerciseId), Mockito.any(PageRequest.class)))
        .thenReturn(syntheticSets);

    double slope = service.computeOverloadTrend(exerciseId, 3);

    assertThat(slope)
        .withFailMessage("Expected a positive slope for increasing weights, but was %f", slope)
        .isGreaterThan(0.0);
  }

  @Test
  void computeOverloadTrend_usesCorrectPageRequestSize() {
    Exercise e1 = new Exercise(UUID.randomUUID(), "Pull Up", "Pull");

    Mockito.when(repo.findSetsByExerciseId(Mockito.eq(exerciseId), Mockito.any(PageRequest.class)))
        .thenReturn(List.of());

    service.computeOverloadTrend(exerciseId, 5);

    ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
    Mockito.verify(repo).findSetsByExerciseId(Mockito.eq(exerciseId), captor.capture());

    assertThat(captor.getValue().getPageSize()).isEqualTo(5);
  }
}
