package com.andremunay.hobbyhub.weightlifting.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.andremunay.hobbyhub.shared.util.NameNormalizer;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link WeightliftingService}, verifying behavior across: - One-rep max
 * computations - Overload trend regression - Workout and set creation - Exercise management - Data
 * mapping and repository interaction
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeightliftingServiceTest {

  @Mock private WorkoutRepository workoutRepo;

  @Mock private ExerciseRepository exerciseRepo;

  private WeightliftingService service;
  private final UUID exerciseId = UUID.randomUUID();
  private final String exerciseName = "Bench Press";
  private final String normalized = NameNormalizer.normalize(exerciseName);

  @BeforeEach
  void setUp() {
    service = new WeightliftingService(workoutRepo, exerciseRepo, new EpleyOneRepMaxStrategy());
    lenient()
        .when(exerciseRepo.findByName(normalized))
        .thenReturn(Optional.of(new Exercise(exerciseId, exerciseName, "x")));
  }

  /** Verifies that a strictly increasing trend of top weights yields a positive slope. */
  @Test
  void computeOverloadTrend_returnsPositiveSlope_forStrictlyIncreasingWeights() {
    // prepare synthetic sets
    Exercise ex = new Exercise(exerciseId, exerciseName, "x");
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 2));
    Workout w3 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 3));
    WorkoutSet s1 = new WorkoutSet(new WorkoutSetId(w1.getId(), 1), ex, BigDecimal.valueOf(50), 5);
    s1.setWorkout(w1);
    WorkoutSet s2 = new WorkoutSet(new WorkoutSetId(w2.getId(), 1), ex, BigDecimal.valueOf(60), 5);
    s2.setWorkout(w2);
    WorkoutSet s3 = new WorkoutSet(new WorkoutSetId(w3.getId(), 1), ex, BigDecimal.valueOf(70), 5);
    s3.setWorkout(w3);
    List<WorkoutSet> syntheticSets = List.of(s1, s2, s3);

    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(syntheticSets);

    double slope = service.computeOverloadTrend(exerciseName, 3);

    assertTrue(slope > 0.0, "Expected positive slope for increasing weights");
  }

  /** Verifies that the correct PageRequest size is passed to the repository. */
  @Test
  void computeOverloadTrend_usesCorrectPageRequestSize() {
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(Collections.emptyList());
    service.computeOverloadTrend(exerciseName, 5);
    ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
    verify(workoutRepo).findSetsByExerciseId(eq(exerciseId), captor.capture());
    assertEquals(5, captor.getValue().getPageSize());
  }

  /** Ensures that the configured strategy is delegated for 1RM calculation. */
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

  /** Verifies correct ordering and mapping in 1RM stats result. */
  @Test
  void getOneRepMaxStats_returnsSortedOneRmPoints() {
    Exercise e = new Exercise(exerciseId, exerciseName, "x");
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 2));
    WorkoutSet s1 = new WorkoutSet(new WorkoutSetId(w1.getId(), 1), e, BigDecimal.valueOf(10), 30);
    s1.setWorkout(w1);
    WorkoutSet s2 = new WorkoutSet(new WorkoutSetId(w2.getId(), 1), e, BigDecimal.valueOf(20), 30);
    s2.setWorkout(w2);
    List<WorkoutSet> sets = List.of(s2, s1);
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class))).thenReturn(sets);

    List<OneRmPointDto> stats = service.getOneRepMaxStats(exerciseName, 2);

    assertEquals(2, stats.size());
    assertEquals(w1.getId(), stats.get(0).getWorkoutId());
    assertEquals(w1.getPerformedOn(), stats.get(0).getDate());
    assertEquals(20.0, stats.get(0).getOneRepMax(), 1e-6);
    assertEquals(40.0, stats.get(1).getOneRepMax(), 1e-6);
  }

  /** Verifies that an exercise is created, saved, and returned with an ID. */
  @Test
  void createExercise_savesExerciseAndReturnsName() {
    ExerciseDto dto = new ExerciseDto();
    dto.setName("Pull Up");
    dto.setMuscleGroup("Back");
    ArgumentCaptor<Exercise> captor = ArgumentCaptor.forClass(Exercise.class);

    String returned = service.createExercise(dto);

    Mockito.verify(exerciseRepo).save(captor.capture());
    Exercise saved = captor.getValue();

    assertEquals(returned, saved.getName());
    assertEquals("pullup", saved.getName());
    assertEquals("Back", saved.getMuscleGroup());
  }

  /** Confirms sets are properly mapped when fetching a workout by ID. */
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

  /** Verifies deletion when the workout exists. */
  @Test
  void deleteWorkout_deletesWhenExists() {
    UUID id = UUID.randomUUID();
    Mockito.when(workoutRepo.existsById(id)).thenReturn(true);

    service.deleteWorkout(id);

    Mockito.verify(workoutRepo).deleteById(id);
  }

  /** Throws an exception when trying to delete a nonexistent workout. */
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

  /** Delegates deletion of an exercise by ID to the repository. */
  @Test
  void deleteExercise_callsRepositoryDelete() {
    service.deleteExercise(exerciseName);
    verify(exerciseRepo)
        .delete(argThat(ex -> ex.getName().equals(exerciseName) && ex.getId().equals(exerciseId)));
  }

  /** Returns DTO-mapped list of all exercises. */
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

  /** Returns all workouts as mapped DTOs including their sets. */
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

  /** Returns 0 slope when no sets are found. */
  @Test
  void computeOverloadTrend_emptySets_returnsZeroSlope() {
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(Collections.emptyList());
    double slope = service.computeOverloadTrend(exerciseName, 5);
    assertEquals(0.0, slope, 0.0001);
  }

  /** Verifies proper sort and mapping order in 1RM stat results. */
  @Test
  void getOneRepMaxStats_mapsAndOrdersCorrectly() {
    // given
    Exercise ex = new Exercise(exerciseId, exerciseName, "legs");
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 2));
    WorkoutSet set1 =
        new WorkoutSet(new WorkoutSetId(w1.getId(), 1), ex, BigDecimal.valueOf(100), 5);
    set1.setWorkout(w1);
    WorkoutSet set2 =
        new WorkoutSet(new WorkoutSetId(w2.getId(), 1), ex, BigDecimal.valueOf(110), 5);
    set2.setWorkout(w2);

    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(List.of(set2, set1));

    // when
    List<OneRmPointDto> stats = service.getOneRepMaxStats(exerciseName, 2);

    // then
    assertEquals(2, stats.size());
    assertEquals(LocalDate.of(2025, 1, 1), stats.get(0).getDate());
    assertEquals(LocalDate.of(2025, 1, 2), stats.get(1).getDate());
  }

  /** Returns an empty list when no sets are available for 1RM calculation. */
  @Test
  void getOneRepMaxStats_emptySets_returnsEmptyList() {
    // stub lookup of the name → entity
    when(exerciseRepo.findByName(eq(NameNormalizer.normalize(exerciseName))))
        .thenReturn(Optional.of(new Exercise(exerciseId, exerciseName, "group")));

    // stub no sets returned
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(Collections.emptyList());

    // now call by name, not UUID
    List<OneRmPointDto> stats = service.getOneRepMaxStats(exerciseName, 3);

    assertTrue(stats.isEmpty());
  }

  /** Throws if workout creation references a nonexistent exercise. */
  @Test
  void createWorkout_missingExercise_throwsNoSuchElement() {
    WorkoutDto req = new WorkoutDto();
    req.setPerformedOn(LocalDate.now());

    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseName("benchpress"); // now supply name instead of ID
    dto.setOrder(1);
    dto.setWeightKg(BigDecimal.TEN);
    dto.setReps(5);
    req.setSets(List.of(dto));

    // stub lookup by name to return empty
    when(exerciseRepo.findByName(NameNormalizer.normalize("benchpress")))
        .thenReturn(Optional.empty());

    NoSuchElementException ex =
        assertThrows(NoSuchElementException.class, () -> service.createWorkout(req));
    assertTrue(ex.getMessage().contains("benchpress"));
  }

  /** Saves an exercise and returns the generated UUID. */
  @Test
  void createExercise_savesAndReturnsName() {
    ExerciseDto dto = new ExerciseDto();
    dto.setName("Deadlift");
    dto.setMuscleGroup("Back");

    service.createExercise(dto);

    ArgumentCaptor<Exercise> cap = ArgumentCaptor.forClass(Exercise.class);
    verify(exerciseRepo).save(cap.capture());
    assertEquals("deadlift", cap.getValue().getName());
  }

  /** Throws if trying to add a set to a missing workout. */
  @Test
  void addSetToWorkout_missingWorkout_throwsEntityNotFound() {
    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseId(exerciseId);
    dto.setOrder(1);
    dto.setWeightKg(BigDecimal.ONE);
    dto.setReps(1);

    when(workoutRepo.findById(any())).thenReturn(Optional.empty());

    EntityNotFoundException ex =
        assertThrows(
            EntityNotFoundException.class, () -> service.addSetToWorkout(UUID.randomUUID(), dto));
    assertTrue(ex.getMessage().contains("Workout not found"));
  }

  /** Returns full mapped workout by ID including set details. */
  @Test
  void getWorkoutWithSets_returnsCorrectDto() {
    UUID wid = UUID.randomUUID();
    Workout w = new Workout(wid, LocalDate.of(2025, 5, 5));
    Exercise exObj = new Exercise(exerciseId, "Press", "Push");
    WorkoutSet set = new WorkoutSet(new WorkoutSetId(wid, 1), exObj, BigDecimal.valueOf(70), 6);
    set.setWorkout(w);
    w.getSets().add(set);
    when(workoutRepo.findById(wid)).thenReturn(Optional.of(w));
    WorkoutDto dto = service.getWorkoutWithSets(wid);
    assertEquals(LocalDate.of(2025, 5, 5), dto.getPerformedOn());
    assertEquals(1, dto.getSets().size());
    WorkoutSetDto sDto = dto.getSets().get(0);
    assertEquals(6, sDto.getReps());
    assertEquals(70, sDto.getWeightKg().intValue());
  }

  /** Throws when requested workout does not exist. */
  @Test
  void getWorkoutWithSets_notFound_throwsEntityNotFound() {
    UUID wid = UUID.randomUUID();
    when(workoutRepo.findById(wid)).thenReturn(Optional.empty());

    EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> service.getWorkoutWithSets(wid));
    assertTrue(ex.getMessage().contains("Workout not found"));
  }

  /** Successfully deletes a workout if it exists. */
  @Test
  void deleteWorkout_success() {
    UUID wid = UUID.randomUUID();
    when(workoutRepo.existsById(wid)).thenReturn(true);

    service.deleteWorkout(wid);
    verify(workoutRepo).deleteById(wid);
  }

  /** Throws if deleting a nonexistent workout. */
  @Test
  void deleteWorkout_notFound_throwsEntityNotFound() {
    UUID wid = UUID.randomUUID();
    when(workoutRepo.existsById(wid)).thenReturn(false);

    EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> service.deleteWorkout(wid));
    assertNotNull(ex.getMessage());
  }

  /** Verifies exercise deletion delegates to repository. */
  @Test
  void deleteExercise_delegatesToRepo() {
    when(exerciseRepo.findByName(eq((exerciseName))))
        .thenReturn(Optional.of(new Exercise(exerciseId, exerciseName, "muscleGroup")));

    // when: delete by name
    service.deleteExercise(exerciseName);

    // then: repository.delete(...) is called with the resolved Exercise
    verify(exerciseRepo)
        .delete(argThat(ex -> ex.getId().equals(exerciseId) && ex.getName().equals(exerciseName)));
  }

  /** Ensures all exercises are mapped correctly to DTOs. */
  @Test
  void getAllExercises_mapsAll() {
    Exercise e1 = new Exercise(UUID.randomUUID(), "A", "X");
    Exercise e2 = new Exercise(UUID.randomUUID(), "B", "Y");
    when(exerciseRepo.findAll()).thenReturn(List.of(e1, e2));

    List<ExerciseDto> dtos = service.getAllExercises();
    assertEquals(2, dtos.size());
    assertEquals("A", dtos.get(0).getName());
    assertEquals("X", dtos.get(0).getMuscleGroup());
    assertEquals("B", dtos.get(1).getName());
    assertEquals("Y", dtos.get(1).getMuscleGroup());
  }

  /** Confirms all workouts and sets are mapped correctly. */
  @Test
  void getAllWorkouts_mapsAll() {
    UUID wid = UUID.randomUUID();
    Workout w = new Workout(wid, LocalDate.of(2025, 6, 6));
    Exercise exObj = new Exercise(exerciseId, "C", "Z");
    WorkoutSet set = new WorkoutSet(new WorkoutSetId(wid, 1), exObj, BigDecimal.valueOf(90), 3);
    set.setWorkout(w);
    w.getSets().add(set);
    when(workoutRepo.findAllWithSets()).thenReturn(List.of(w));
    var dtos = service.getAllWorkouts();
    assertEquals(1, dtos.size());
  }

  /** Verifies 1RM is computed using the Epley formula. */
  @Test
  void calculateOneRepMax_appliesEpleyFormula() {
    UUID wid = UUID.randomUUID();
    Exercise ex = new Exercise(exerciseId, "Test", "Group");
    Workout w = new Workout(wid, LocalDate.now());
    WorkoutSet set = new WorkoutSet(new WorkoutSetId(wid, 1), ex, BigDecimal.valueOf(100), 5);
    set.setWorkout(w);

    double result = service.calculateOneRepMax(set);

    assertEquals(100 * (1 + 5.0 / 30), result, 0.0001);
  }

  /** Throws when trying to compute 1RM with zero reps. */
  @Test
  void calculateOneRepMax_zeroReps_throwsIllegalArgument() {
    UUID wid = UUID.randomUUID();
    Exercise ex = new Exercise(exerciseId, "Test", "Group");
    Workout w = new Workout(wid, LocalDate.now());
    WorkoutSet set = new WorkoutSet(new WorkoutSetId(wid, 1), ex, BigDecimal.valueOf(50), 0);
    set.setWorkout(w);

    IllegalArgumentException exThrown =
        assertThrows(IllegalArgumentException.class, () -> service.calculateOneRepMax(set));
    assertNotNull(exThrown.getMessage());
  }

  /** Returns a negative slope when weights are strictly decreasing. */
  @Test
  void computeOverloadTrend_decreasingWeights_returnsNegativeSlope() {
    // arrange: lookup by name yields our Exercise
    when(exerciseRepo.findByName(eq(NameNormalizer.normalize(exerciseName))))
        .thenReturn(Optional.of(new Exercise(exerciseId, exerciseName, "TestGroup")));

    // arrange: create three workouts with strictly decreasing weights
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 2));
    Workout w3 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 3));

    Exercise ex = new Exercise(exerciseId, exerciseName, "TestGroup");
    WorkoutSet s1 = new WorkoutSet(new WorkoutSetId(w1.getId(), 1), ex, BigDecimal.valueOf(70), 5);
    WorkoutSet s2 = new WorkoutSet(new WorkoutSetId(w2.getId(), 1), ex, BigDecimal.valueOf(60), 5);
    WorkoutSet s3 = new WorkoutSet(new WorkoutSetId(w3.getId(), 1), ex, BigDecimal.valueOf(50), 5);
    s1.setWorkout(w1);
    s2.setWorkout(w2);
    s3.setWorkout(w3);

    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(List.of(s1, s2, s3));

    // act: call by exerciseName
    double slope = service.computeOverloadTrend(exerciseName, 3);

    // assert: slope is negative
    assertTrue(
        slope < 0.0,
        String.format("Expected a negative slope for decreasing weights, but was %f", slope));
  }

  /** Allows saving a workout with no sets. */
  @Test
  void createWorkout_emptySets_savesWorkoutWithoutSets() {
    WorkoutDto req = new WorkoutDto();
    req.setPerformedOn(LocalDate.now());
    req.setSets(Collections.emptyList());

    UUID returned = service.createWorkout(req);

    ArgumentCaptor<Workout> captor = ArgumentCaptor.forClass(Workout.class);
    verify(workoutRepo).save(captor.capture());
    Workout saved = captor.getValue();

    assertEquals(returned, saved.getId());
    assertEquals(req.getPerformedOn(), saved.getPerformedOn());
    assertEquals(0, saved.getSets().size());
    verifyNoInteractions(exerciseRepo);
  }

  /** Groups by workout ID and limits to the latest N sessions. */
  @Test
  void computeOverloadTrend_groupsByWorkoutId_and_limitsToLastN() {
    // arrange: resolve name → Exercise
    when(exerciseRepo.findByName(eq(NameNormalizer.normalize(exerciseName))))
        .thenReturn(Optional.of(new Exercise(exerciseId, exerciseName, "Group")));

    // arrange: two workouts, multiple sets in the first workout
    UUID wid1 = UUID.randomUUID();
    UUID wid2 = UUID.randomUUID();
    Exercise ex = new Exercise(exerciseId, exerciseName, "Group");
    Workout w1 = new Workout(wid1, LocalDate.of(2025, 1, 1));
    Workout w2 = new Workout(wid2, LocalDate.of(2025, 1, 2));
    WorkoutSet s1a = new WorkoutSet(new WorkoutSetId(wid1, 1), ex, BigDecimal.valueOf(50), 5);
    WorkoutSet s1b = new WorkoutSet(new WorkoutSetId(wid1, 2), ex, BigDecimal.valueOf(70), 5);
    WorkoutSet s2 = new WorkoutSet(new WorkoutSetId(wid2, 1), ex, BigDecimal.valueOf(60), 5);
    s1a.setWorkout(w1);
    s1b.setWorkout(w1);
    s2.setWorkout(w2);
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(List.of(s1a, s1b, s2));

    // act: call by name, limit to last 2 workouts
    double slope = service.computeOverloadTrend(exerciseName, 2);

    // assert: negative slope after grouping and limiting to lastN
    assertTrue(
        slope < 0.0,
        String.format(
            "Expected a negative slope after grouping and limiting lastN, but was %f", slope));
  }

  /** Returns 0 slope when there's only one data point. */
  @Test
  void computeOverloadTrend_singlePoint_returnsZeroSlope() {
    // arrange: resolve name → Exercise
    when(exerciseRepo.findByName(eq(NameNormalizer.normalize(exerciseName))))
        .thenReturn(Optional.of(new Exercise(exerciseId, exerciseName, "TG")));

    // arrange: one workout set only
    UUID wid = UUID.randomUUID();
    Workout w = new Workout(wid, LocalDate.now());
    WorkoutSet single =
        new WorkoutSet(
            new WorkoutSetId(wid, 1),
            new Exercise(exerciseId, exerciseName, "TG"),
            BigDecimal.valueOf(100),
            5);
    single.setWorkout(w);
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(List.of(single));

    // act: call by name, not UUID
    double slope = service.computeOverloadTrend(exerciseName, 1);

    // assert
    assertEquals(0.0, slope, "With only one data point, slope should be 0");
  }

  /* Verifies that computeOverloadTrend throws EntityNotFoundException for an unknown exercise name */
  @Test
  void computeOverloadTrend_unknownExercise_throwsEntityNotFound() {
    String unknown = NameNormalizer.normalize("nonexistent");
    when(exerciseRepo.findByName(unknown)).thenReturn(Optional.empty());

    EntityNotFoundException ex =
        assertThrows(
            EntityNotFoundException.class, () -> service.computeOverloadTrend("nonexistent", 3));
    assertNotNull(ex.getMessage());
  }

  /* Verifies that getOneRepMaxStats throws EntityNotFoundException for an unknown exercise name */
  @Test
  void getOneRepMaxStats_unknownExercise_throwsEntityNotFound() {
    String unknown = NameNormalizer.normalize("nonexistent");
    when(exerciseRepo.findByName(unknown)).thenReturn(Optional.empty());

    EntityNotFoundException ex =
        assertThrows(
            EntityNotFoundException.class, () -> service.getOneRepMaxStats("nonexistent", 3));
    assertNotNull(ex.getMessage());
  }

  /* Verifies that createExercise throws ResponseStatusException with CONFLICT when the exercise already exists */
  @Test
  void createExercise_conflict_throwsResponseStatusException() {
    ExerciseDto dto = new ExerciseDto();
    dto.setName(exerciseName);
    dto.setMuscleGroup("Chest");
    String nameNorm = NameNormalizer.normalize(exerciseName);
    when(exerciseRepo.existsByName(eq(nameNorm))).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> service.createExercise(dto));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    String reason = Objects.requireNonNull(ex.getReason(), "Exception reason should not be null");
    assertTrue(
        reason.contains(exerciseName), "Reason should mention the conflicting exercise name");
  }

  /* Verifies that addSetToWorkout throws EntityNotFoundException when the referenced exercise does not exist */
  @Test
  void addSetToWorkout_unknownExercise_throwsEntityNotFound() {
    // given an existing workout
    UUID workoutId = UUID.randomUUID();
    Workout workout = new Workout(workoutId, LocalDate.now());
    when(workoutRepo.findById(workoutId)).thenReturn(Optional.of(workout));

    // and a DTO with a nonexistent exercise name
    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseName("badname");
    dto.setOrder(1);
    dto.setWeightKg(BigDecimal.valueOf(100));
    dto.setReps(5);
    String badNorm = NameNormalizer.normalize("badname");
    when(exerciseRepo.findByName(eq(badNorm))).thenReturn(Optional.empty());

    // when & then: expect EntityNotFoundException
    EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> service.addSetToWorkout(workoutId, dto));
    assertTrue(
        ex.getMessage().contains("badname"),
        "Exception message should mention the missing exercise name");
  }

  /* Verifies that deleteExercise throws EntityNotFoundException when attempting to delete a nonexistent exercise */
  @Test
  void deleteExercise_unknownExercise_throwsEntityNotFound() {
    // stub lookup to return empty for nonexistent exercise
    String unknown = NameNormalizer.normalize("nonexistent");
    when(exerciseRepo.findByName(eq(unknown))).thenReturn(Optional.empty());

    // assert that deleting a nonexistent exercise throws EntityNotFoundException
    EntityNotFoundException ex =
        assertThrows(EntityNotFoundException.class, () -> service.deleteExercise("nonexistent"));
    assertTrue(
        ex.getMessage().contains("nonexistent"),
        "Exception message should mention the missing exercise name");
  }

  /* Verifies that createWorkout correctly maps and persists a workout with multiple sets */
  @Test
  void createWorkout_withSets_savesWorkoutAndSets() {
    // Build the incoming DTO with existing exercises
    WorkoutDto dto = new WorkoutDto();
    LocalDate performedOn = LocalDate.of(2025, 1, 1);
    dto.setPerformedOn(performedOn);

    WorkoutSetDto set1 = new WorkoutSetDto();
    set1.setExerciseName("Back Squat");
    set1.setOrder(1);
    set1.setWeightKg(BigDecimal.valueOf(100));
    set1.setReps(5);

    WorkoutSetDto set2 = new WorkoutSetDto();
    set2.setExerciseName("Deadlift");
    set2.setOrder(2);
    set2.setWeightKg(BigDecimal.valueOf(150));
    set2.setReps(3);

    dto.setSets(List.of(set1, set2));

    // Stub repository lookups for existing exercises
    UUID backSquatId = UUID.randomUUID();
    Exercise backSquat = new Exercise(backSquatId, NameNormalizer.normalize("Back Squat"), "Legs");
    when(exerciseRepo.findByName(eq(NameNormalizer.normalize("Back Squat"))))
        .thenReturn(Optional.of(backSquat));

    UUID deadliftId = UUID.randomUUID();
    Exercise deadlift = new Exercise(deadliftId, NameNormalizer.normalize("Deadlift"), "Back");
    when(exerciseRepo.findByName(eq(NameNormalizer.normalize("Deadlift"))))
        .thenReturn(Optional.of(deadlift));

    // Capture and verify saved Workout
    ArgumentCaptor<Workout> captor = ArgumentCaptor.forClass(Workout.class);

    UUID returnedId = service.createWorkout(dto);

    verify(workoutRepo).save(captor.capture());
    Workout saved = captor.getValue();

    // Verify metadata
    assertEquals(returnedId, saved.getId());
    assertEquals(performedOn, saved.getPerformedOn());
    assertEquals(2, saved.getSets().size());

    Map<Integer, WorkoutSet> byOrder =
        saved.getSets().stream().collect(Collectors.toMap(ws -> ws.getId().getOrder(), ws -> ws));

    WorkoutSet ws1 = byOrder.get(1);
    assertNotNull(ws1);
    assertEquals(100, ws1.getWeightKg().intValue());
    assertEquals(5, ws1.getReps());
    assertEquals(NameNormalizer.normalize("Back Squat"), ws1.getExercise().getName());

    WorkoutSet ws2 = byOrder.get(2);
    assertNotNull(ws2);
    assertEquals(150, ws2.getWeightKg().intValue());
    assertEquals(3, ws2.getReps());
    assertEquals(NameNormalizer.normalize("Deadlift"), ws2.getExercise().getName());
  }

  /* Verifies that addSetToWorkout correctly adds and saves a new set for an existing workout and exercise */
  @Test
  void addSetToWorkout_validWorkoutAndExercise_savesNewSet() {
    // given: an existing workout with no sets
    UUID workoutId = UUID.randomUUID();
    Workout workout = new Workout(workoutId, LocalDate.of(2025, 2, 2));
    when(workoutRepo.findById(workoutId)).thenReturn(Optional.of(workout));

    // and an existing exercise lookup
    String rawName = "Bench Press";
    String normName = NameNormalizer.normalize(rawName);
    Exercise exercise = new Exercise(exerciseId, normName, "Push");
    when(exerciseRepo.findByName(eq(normName))).thenReturn(Optional.of(exercise));

    // and a DTO describing the new set
    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseName(rawName);
    dto.setOrder(1);
    dto.setWeightKg(BigDecimal.valueOf(80));
    dto.setReps(8);

    // capture the Workout saved to the repo
    ArgumentCaptor<Workout> cap = ArgumentCaptor.forClass(Workout.class);

    // when
    service.addSetToWorkout(workoutId, dto);

    // then: verify save and inspect the added set
    verify(workoutRepo).save(cap.capture());
    Workout saved = cap.getValue();
    assertEquals(1, saved.getSets().size(), "Should have exactly one set");

    WorkoutSet added = saved.getSets().iterator().next();
    assertEquals(1, added.getId().getOrder());
    assertEquals(80, added.getWeightKg().intValue());
    assertEquals(8, added.getReps());
    assertEquals(exercise, added.getExercise());
  }

  /* Verifies that getAllExercises returns an empty list when no exercises are present */
  @Test
  void getAllExercises_emptyList_returnsEmptyList() {
    // stub findAll to return empty list
    when(exerciseRepo.findAll()).thenReturn(Collections.emptyList());

    List<ExerciseDto> dtos = service.getAllExercises();
    assertNotNull(dtos, "Result should not be null");
    assertTrue(dtos.isEmpty(), "Expected empty list when no exercises exist");
  }

  /* Verifies that getAllWorkouts returns an empty list when no workouts are present */
  @Test
  void getAllWorkouts_emptyList_returnsEmptyList() {
    // stub repository to return no workouts
    when(workoutRepo.findAllWithSets()).thenReturn(Collections.emptyList());

    // execute
    List<WorkoutDto> dtos = service.getAllWorkouts();

    // verify
    assertNotNull(dtos, "Result should not be null");
    assertTrue(dtos.isEmpty(), "Expected empty list when no workouts exist");
  }
}
