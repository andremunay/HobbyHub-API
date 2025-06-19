package com.andremunay.hobbyhub.weightlifting.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSetId;
import com.andremunay.hobbyhub.weightlifting.infra.ExerciseRepository;
import com.andremunay.hobbyhub.weightlifting.infra.WorkoutRepository;
import com.andremunay.hobbyhub.weightlifting.infra.dto.ExerciseDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.OneRmPointDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutSetDto;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WeightliftingServiceTest {

  @Mock private WorkoutRepository workoutRepo;

  @Mock private ExerciseRepository exerciseRepo;

  private WeightliftingService service;
  private final UUID exerciseId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
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

    Mockito.when(
            workoutRepo.findSetsByExerciseId(
                Mockito.eq(exerciseId), Mockito.any(PageRequest.class)))
        .thenReturn(syntheticSets);

    double slope = service.computeOverloadTrend(exerciseId, 3);

    assertThat(slope)
        .withFailMessage("Expected a positive slope for increasing weights, but was %f", slope)
        .isGreaterThan(0.0);
  }

  @Test
  void computeOverloadTrend_usesCorrectPageRequestSize() {

    Mockito.when(
            workoutRepo.findSetsByExerciseId(
                Mockito.eq(exerciseId), Mockito.any(PageRequest.class)))
        .thenReturn(List.of());

    service.computeOverloadTrend(exerciseId, 5);

    ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
    Mockito.verify(workoutRepo).findSetsByExerciseId(Mockito.eq(exerciseId), captor.capture());

    assertThat(captor.getValue().getPageSize()).isEqualTo(5);
  }

  @Test
  void addSetToWorkout_addsNewSetCorrectly() {
    // Arrange
    UUID workoutId = UUID.randomUUID();
    Exercise e1 = new Exercise(exerciseId, "Bench Press", "Push");
    Workout workout = new Workout(workoutId, LocalDate.now());

    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseId(e1.getId());
    dto.setWeightKg(BigDecimal.valueOf(100));
    dto.setReps(5);
    dto.setOrder(1);

    Mockito.when(workoutRepo.findById(workoutId)).thenReturn(Optional.of(workout));
    Mockito.when(exerciseRepo.getReferenceById(exerciseId)).thenReturn(e1);

    // Act
    service.addSetToWorkout(workoutId, dto);

    // Assert
    assertEquals(1, workout.getSets().size());
    WorkoutSet added = workout.getSets().get(0);
    assertEquals(e1, added.getExercise());
    assertEquals(0, added.getWeightKg().compareTo(BigDecimal.valueOf(100)));
    assertEquals(5, added.getReps());
    assertEquals(1, added.getId().getOrder());
    assertEquals(workoutId, added.getId().getWorkoutId());

    // Optional: verify save was called
    Mockito.verify(workoutRepo).save(workout);
  }

  @Test
  void calculateOneRepMax_delegatesToStrategy() {
    OneRepMaxStrategy mockStrategy = Mockito.mock(OneRepMaxStrategy.class);
    WeightliftingService localService =
        new WeightliftingService(workoutRepo, exerciseRepo, mockStrategy);
    WorkoutSet set =
        new WorkoutSet(
            new WorkoutSetId(UUID.randomUUID(), 1),
            new Exercise(exerciseId, "Bench Press", "Push"),
            BigDecimal.valueOf(70),
            5);
    Mockito.when(mockStrategy.calculate(70.0, 5)).thenReturn(123.45);

    double result = localService.calculateOneRepMax(set);

    assertEquals(123.45, result);
    Mockito.verify(mockStrategy).calculate(70.0, 5);
  }

  @Test
  void getOneRepMaxStats_returnsSortedOneRmPoints() {
    Exercise e = new Exercise(exerciseId, "Squat", "Legs");
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 2));
    WorkoutSet s1 = new WorkoutSet(new WorkoutSetId(w1.getId(), 1), e, BigDecimal.valueOf(10), 30);
    s1.setWorkout(w1);
    WorkoutSet s2 = new WorkoutSet(new WorkoutSetId(w2.getId(), 1), e, BigDecimal.valueOf(20), 30);
    s2.setWorkout(w2);
    List<WorkoutSet> sets = List.of(s2, s1);
    Mockito.when(
            workoutRepo.findSetsByExerciseId(
                Mockito.eq(exerciseId), Mockito.any(PageRequest.class)))
        .thenReturn(sets);

    List<OneRmPointDto> stats = service.getOneRepMaxStats(exerciseId, 2);

    assertEquals(2, stats.size());
    OneRmPointDto p1 = stats.get(0);
    assertEquals(w1.getId(), p1.getWorkoutId());
    assertEquals(w1.getPerformedOn(), p1.getDate());
    assertEquals(20.0, p1.getOneRepMax(), 0.0001);

    OneRmPointDto p2 = stats.get(1);
    assertEquals(w2.getId(), p2.getWorkoutId());
    assertEquals(w2.getPerformedOn(), p2.getDate());
    assertEquals(40.0, p2.getOneRepMax(), 0.0001);
  }

  @Test
  void createWorkout_savesWorkoutAndReturnsId() {
    Exercise e = new Exercise(exerciseId, "Deadlift", "Back");
    WorkoutDto req = new WorkoutDto();
    LocalDate date = LocalDate.of(2025, 6, 15);
    req.setPerformedOn(date);
    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseId(exerciseId);
    dto.setWeightKg(BigDecimal.valueOf(80));
    dto.setReps(5);
    dto.setOrder(1);
    req.setSets(List.of(dto));

    Mockito.when(exerciseRepo.findById(exerciseId)).thenReturn(Optional.of(e));
    ArgumentCaptor<Workout> captor = ArgumentCaptor.forClass(Workout.class);

    UUID returned = service.createWorkout(req);

    Mockito.verify(workoutRepo).save(captor.capture());
    Workout saved = captor.getValue();

    assertEquals(returned, saved.getId());
    assertEquals(date, saved.getPerformedOn());
    assertEquals(1, saved.getSets().size());
    WorkoutSet savedSet = saved.getSets().get(0);
    assertEquals(e, savedSet.getExercise());
    assertEquals(0, savedSet.getWeightKg().compareTo(BigDecimal.valueOf(80)));
    assertEquals(5, savedSet.getReps());
    assertEquals(1, savedSet.getId().getOrder());
  }

  @Test
  void createExercise_savesExerciseAndReturnsId() {
    ExerciseDto dto = new ExerciseDto();
    dto.setName("Pull Up");
    dto.setMuscleGroup("Back");
    ArgumentCaptor<Exercise> captor = ArgumentCaptor.forClass(Exercise.class);

    UUID returned = service.createExercise(dto);

    Mockito.verify(exerciseRepo).save(captor.capture());
    Exercise saved = captor.getValue();

    assertEquals(returned, saved.getId());
    assertEquals("Pull Up", saved.getName());
    assertEquals("Back", saved.getMuscleGroup());
  }

  @Test
  void getWorkoutWithSets_returnsDtoWithSets() {
    UUID workoutId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2025, 6, 15);
    Exercise e = new Exercise(exerciseId, "Bench Press", "Chest");
    Workout workout = new Workout(workoutId, date);
    WorkoutSet set = new WorkoutSet(new WorkoutSetId(workoutId, 1), e, BigDecimal.valueOf(100), 5);
    set.setWorkout(workout);
    workout.addSet(set);
    Mockito.when(workoutRepo.findById(workoutId)).thenReturn(Optional.of(workout));

    WorkoutDto dto = service.getWorkoutWithSets(workoutId);

    assertEquals(date, dto.getPerformedOn());
    assertEquals(1, dto.getSets().size());
    WorkoutSetDto sDto = dto.getSets().get(0);
    assertEquals(e.getId(), sDto.getExerciseId());
    assertEquals(0, sDto.getWeightKg().compareTo(BigDecimal.valueOf(100)));
    assertEquals(5, sDto.getReps());
    assertEquals(1, sDto.getOrder());
  }

  @Test
  void deleteWorkout_deletesWhenExists() {
    UUID id = UUID.randomUUID();
    Mockito.when(workoutRepo.existsById(id)).thenReturn(true);

    service.deleteWorkout(id);

    Mockito.verify(workoutRepo).deleteById(id);
  }

  @Test
  void deleteWorkout_throwsException_whenNotFound() {
    UUID id = UUID.randomUUID();
    Mockito.when(workoutRepo.existsById(id)).thenReturn(false);
    try {
      service.deleteWorkout(id);
      fail("Expected EntityNotFoundException");
    } catch (EntityNotFoundException ex) {
      assertEquals("Workout not found: " + id, ex.getMessage());
    }
  }

  @Test
  void deleteExercise_callsRepositoryDelete() {
    UUID id = UUID.randomUUID();

    service.deleteExercise(id);

    Mockito.verify(exerciseRepo).deleteById(id);
  }

  @Test
  void getAllExercises_returnsDtoList() {
    Exercise e1 = new Exercise(UUID.randomUUID(), "Squat", "Legs");
    Exercise e2 = new Exercise(UUID.randomUUID(), "Deadlift", "Back");
    Mockito.when(exerciseRepo.findAll()).thenReturn(List.of(e1, e2));

    List<ExerciseDto> dtos = service.getAllExercises();

    assertEquals(2, dtos.size());
    ExerciseDto dto1 = dtos.get(0);
    assertEquals(e1.getId(), dto1.getId());
    assertEquals(e1.getName(), dto1.getName());
    assertEquals(e1.getMuscleGroup(), dto1.getMuscleGroup());
    ExerciseDto dto2 = dtos.get(1);
    assertEquals(e2.getId(), dto2.getId());
    assertEquals(e2.getName(), dto2.getName());
    assertEquals(e2.getMuscleGroup(), dto2.getMuscleGroup());
  }

  @Test
  void getAllWorkouts_returnsDtoList() {
    UUID workoutId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2025, 5, 1);
    Exercise e = new Exercise(exerciseId, "Bench Press", "Chest");
    Workout workout = new Workout(workoutId, date);
    WorkoutSet set = new WorkoutSet(new WorkoutSetId(workoutId, 1), e, BigDecimal.valueOf(100), 5);
    set.setWorkout(workout);
    workout.addSet(set);
    Mockito.when(workoutRepo.findAllWithSets()).thenReturn(List.of(workout));

    List<WorkoutDto> dtos = service.getAllWorkouts();

    assertEquals(1, dtos.size());
    WorkoutDto dto = dtos.get(0);
    assertEquals(date, dto.getPerformedOn());
    assertEquals(1, dto.getSets().size());
    WorkoutSetDto sDto = dto.getSets().get(0);
    assertEquals(1, sDto.getOrder());
    assertEquals(0, sDto.getWeightKg().compareTo(BigDecimal.valueOf(100)));
    assertEquals(5, sDto.getReps());
    assertEquals(e.getId(), sDto.getExerciseId());
  }
}
