package com.andremunay.hobbyhub.weightlifting.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for {@link WeightliftingService}, verifying behavior across: - One-rep max
 * computations - Overload trend regression - Workout and set creation - Exercise management - Data
 * mapping and repository interaction
 */
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

  /** Verifies that a strictly increasing trend of top weights yields a positive slope. */
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
    WorkoutSet s3 = new WorkoutSet(new WorkoutSetId(w3.getId(), 1), e1, BigDecimal.valueOf(70), 5);
    s3.setWorkout(w3);

    List<WorkoutSet> syntheticSets = List.of(s1, s2, s3);

    Mockito.when(
            workoutRepo.findSetsByExerciseId(
                Mockito.eq(exerciseId), Mockito.any(PageRequest.class)))
        .thenReturn(syntheticSets);

    double slope = service.computeOverloadTrend(exerciseId, 3);

    assertTrue(
        slope > 0.0,
        String.format("Expected a positive slope for increasing weights, but was %f", slope));
  }

  /** Verifies that the correct PageRequest size is passed to the repository. */
  @Test
  void computeOverloadTrend_usesCorrectPageRequestSize() {

    Mockito.when(
            workoutRepo.findSetsByExerciseId(
                Mockito.eq(exerciseId), Mockito.any(PageRequest.class)))
        .thenReturn(List.of());

    service.computeOverloadTrend(exerciseId, 5);

    ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
    Mockito.verify(workoutRepo).findSetsByExerciseId(Mockito.eq(exerciseId), captor.capture());

    assertEquals(5, captor.getValue().getPageSize());
  }

  /** Tests that adding a set to a workout links it correctly and persists the change. */
  @Test
  void addSetToWorkout_addsNewSetCorrectly() {
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

    service.addSetToWorkout(workoutId, dto);

    assertEquals(1, workout.getSets().size());
    WorkoutSet added = workout.getSets().get(0);
    assertEquals(e1, added.getExercise());
    assertEquals(0, added.getWeightKg().compareTo(BigDecimal.valueOf(100)));
    assertEquals(5, added.getReps());
    assertEquals(1, added.getId().getOrder());
    assertEquals(workoutId, added.getId().getWorkoutId());

    Mockito.verify(workoutRepo).save(workout);
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

  /** Ensures a new workout and set can be created and saved correctly. */
  @Test
  void createWorkout_savesWorkoutAndReturnsId() {
    WorkoutDto req = new WorkoutDto();
    req.setPerformedOn(LocalDate.now());
    WorkoutSetDto setDto = new WorkoutSetDto();
    setDto.setExerciseId(exerciseId);
    setDto.setOrder(1);
    setDto.setWeightKg(BigDecimal.valueOf(80));
    setDto.setReps(5);
    req.setSets(List.of(setDto));

    Exercise exercise = new Exercise(exerciseId, "Bench", "Push");
    when(exerciseRepo.findById(exerciseId)).thenReturn(Optional.of(exercise));

    UUID returned = service.createWorkout(req);

    ArgumentCaptor<Workout> captor = ArgumentCaptor.forClass(Workout.class);
    verify(workoutRepo).save(captor.capture());
    Workout saved = captor.getValue();

    assertEquals(saved.getId(), returned);
    assertEquals(req.getPerformedOn(), saved.getPerformedOn());
    assertEquals(1, saved.getSets().size());
    WorkoutSet savedSet = saved.getSets().get(0);
    assertEquals(80, savedSet.getWeightKg().intValue());
  }

  /** Verifies that an exercise is created, saved, and returned with an ID. */
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
    UUID id = UUID.randomUUID();

    service.deleteExercise(id);

    Mockito.verify(exerciseRepo).deleteById(id);
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

    double slope = service.computeOverloadTrend(exerciseId, 5);

    assertEquals(0.0, slope, 0.0001);
  }

  /** Verifies proper sort and mapping order in 1RM stat results. */
  @Test
  void getOneRepMaxStats_mapsAndOrdersCorrectly() {
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 2));
    Exercise ex = new Exercise(exerciseId, "Squat", "Legs");

    WorkoutSet set1 =
        new WorkoutSet(new WorkoutSetId(w1.getId(), 1), ex, BigDecimal.valueOf(100), 5);
    set1.setWorkout(w1);
    WorkoutSet set2 =
        new WorkoutSet(new WorkoutSetId(w2.getId(), 1), ex, BigDecimal.valueOf(110), 5);
    set2.setWorkout(w2);

    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(List.of(set2, set1));

    List<OneRmPointDto> stats = service.getOneRepMaxStats(exerciseId, 2);

    assertEquals(2, stats.size());
    assertEquals(LocalDate.of(2025, 1, 1), stats.get(0).getDate());
    assertEquals(LocalDate.of(2025, 1, 2), stats.get(1).getDate());
  }

  /** Returns an empty list when no sets are available for 1RM calculation. */
  @Test
  void getOneRepMaxStats_emptySets_returnsEmptyList() {
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(Collections.emptyList());

    List<OneRmPointDto> stats = service.getOneRepMaxStats(exerciseId, 3);
    assertTrue(stats.isEmpty());
  }

  /** Throws if workout creation references a nonexistent exercise. */
  @Test
  void createWorkout_missingExercise_throwsNoSuchElement() {
    WorkoutDto req = new WorkoutDto();
    req.setPerformedOn(LocalDate.now());
    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseId(exerciseId);
    dto.setOrder(1);
    dto.setWeightKg(BigDecimal.TEN);
    dto.setReps(5);
    req.setSets(List.of(dto));
    when(exerciseRepo.findById(exerciseId)).thenReturn(Optional.empty());
    NoSuchElementException ex =
        assertThrows(NoSuchElementException.class, () -> service.createWorkout(req));
    assertTrue(ex.getMessage().contains(exerciseId.toString()));
  }

  /** Saves an exercise and returns the generated UUID. */
  @Test
  void createExercise_savesAndReturnsId() {
    ExerciseDto dto = new ExerciseDto();
    dto.setName("Deadlift");
    dto.setMuscleGroup("Back");

    UUID id = service.createExercise(dto);

    ArgumentCaptor<Exercise> cap = ArgumentCaptor.forClass(Exercise.class);
    verify(exerciseRepo).save(cap.capture());
    assertEquals("Deadlift", cap.getValue().getName());
    assertEquals(cap.getValue().getId(), id);
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
    UUID exId = UUID.randomUUID();
    service.deleteExercise(exId);
    verify(exerciseRepo).deleteById(exId);
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
    Exercise ex = new Exercise(exerciseId, "Test", "TestGroup");
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 2));
    Workout w3 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 1, 3));

    WorkoutSet s1 =
        new WorkoutSet(new WorkoutSetId(UUID.randomUUID(), 1), ex, BigDecimal.valueOf(70), 5);
    WorkoutSet s2 =
        new WorkoutSet(new WorkoutSetId(UUID.randomUUID(), 2), ex, BigDecimal.valueOf(60), 5);
    WorkoutSet s3 =
        new WorkoutSet(new WorkoutSetId(UUID.randomUUID(), 3), ex, BigDecimal.valueOf(50), 5);
    s1.setWorkout(w1);
    s2.setWorkout(w2);
    s3.setWorkout(w3);
    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(List.of(s1, s2, s3));

    double slope = service.computeOverloadTrend(exerciseId, 3);

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
    UUID wid1 = UUID.randomUUID();
    UUID wid2 = UUID.randomUUID();
    Exercise ex = new Exercise(exerciseId, "Test", "Group");
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

    double slope = service.computeOverloadTrend(exerciseId, 2);
    assertTrue(
        slope < 0.0,
        String.format(
            "Expected a negative slope after grouping and limiting lastN, but was %f", slope));
  }

  /** Successfully saves and verifies a new set is added. */
  @Test
  void addSetToWorkout_success_savesNewSet() {
    UUID wid = UUID.randomUUID();
    Workout workout = new Workout(wid, LocalDate.now());
    when(workoutRepo.findById(wid)).thenReturn(Optional.of(workout));
    Exercise exObj = new Exercise(exerciseId, "Deadlift", "Leg");
    when(exerciseRepo.getReferenceById(exerciseId)).thenReturn(exObj);
    WorkoutSetDto dto = new WorkoutSetDto();
    dto.setExerciseId(exerciseId);
    dto.setOrder(1);
    dto.setWeightKg(BigDecimal.valueOf(90));
    dto.setReps(3);
    service.addSetToWorkout(wid, dto);
    ArgumentCaptor<Workout> capW = ArgumentCaptor.forClass(Workout.class);
    verify(workoutRepo).save(capW.capture());
    Workout saved = capW.getValue();
    assertEquals(1, saved.getSets().size());
    WorkoutSet s = saved.getSets().iterator().next();
    assertEquals(exerciseId, s.getExercise().getId());
    assertEquals(3, s.getReps());
  }

  /** Returns 0 slope when there's only one data point. */
  @Test
  void computeOverloadTrend_singlePoint_returnsZeroSlope() {
    UUID wid = UUID.randomUUID();
    Exercise ex = new Exercise(exerciseId, "Test", "TG");
    Workout w = new Workout(wid, LocalDate.now());
    WorkoutSet single = new WorkoutSet(new WorkoutSetId(wid, 1), ex, BigDecimal.valueOf(100), 5);
    single.setWorkout(w);

    when(workoutRepo.findSetsByExerciseId(eq(exerciseId), any(PageRequest.class)))
        .thenReturn(List.of(single));

    double slope = service.computeOverloadTrend(exerciseId, 1);

    assertEquals(0.0, slope, "With only one data point, slope should be 0");
  }
}
