package com.andremunay.hobbyhub.weightlifting.app;

import static com.andremunay.hobbyhub.shared.util.NameNormalizer.normalize;

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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for managing workouts, exercises, and calculating performance metrics.
 *
 * <p>Handles persistence operations and applies training heuristics such as overload trend and
 * one-rep max.
 */
@Service
@RequiredArgsConstructor
public class WeightliftingService {

  private final WorkoutRepository workoutRepo;
  private final ExerciseRepository exerciseRepo;
  private final OneRepMaxStrategy ormStrategy;

  /**
   * Calculates the estimated one-rep max using the configured formula strategy.
   *
   * @param set a completed workout set
   * @return estimated one-rep max for the given set
   */
  public double calculateOneRepMax(WorkoutSet set) {
    return ormStrategy.calculate(set.getWeightKg().doubleValue(), set.getReps());
  }

  /**
   * Estimates the slope of progressive overload over the most recent N workouts, looked up by
   * human-friendly exercise name.
   *
   * @param exerciseName the exercise to analyze (e.g. "benchpress" or "Bench Press")
   * @param lastN number of most recent workouts to include
   * @return positive/negative slope of weight progression (0.0 if insufficient data)
   */
  @Transactional
  public double computeOverloadTrend(String exerciseName, int lastN) {
    // 1) Normalize & lookup
    String normalized = NameNormalizer.normalize(exerciseName);
    Exercise ex =
        exerciseRepo
            .findByName(normalized)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Unknown exercise: \""
                            + exerciseName
                            + "\". Valid options: "
                            + exerciseRepo.findAll().stream()
                                .map(Exercise::getName)
                                .collect(Collectors.joining(", "))));
    UUID exerciseId = ex.getId();

    // 2) Pull the last N sets for that exercise
    Pageable page = PageRequest.of(0, lastN);
    List<WorkoutSet> sets = workoutRepo.findSetsByExerciseId(exerciseId, page);

    // 3) For each workout keep only the heaviest set, then sort by date
    List<WorkoutSet> topSets =
        sets.stream()
            .collect(
                Collectors.groupingBy(
                    ws -> ws.getWorkout().getId(),
                    Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparing(WorkoutSet::getWeightKg)),
                        Optional::get)))
            .values()
            .stream()
            .sorted(Comparator.comparing(ws -> ws.getWorkout().getPerformedOn()))
            .limit(lastN)
            .toList();

    // 4) If we don’t have at least two data points, slope is zero
    if (topSets.size() < 2) {
      return 0.0;
    }

    // 5) Feed into linear regression
    SimpleRegression regression = new SimpleRegression();
    for (int i = 0; i < topSets.size(); i++) {
      double weight = topSets.get(i).getWeightKg().doubleValue();
      regression.addData(i, weight);
    }

    return regression.getSlope();
  }

  /**
   * Calculates estimated 1RM for top sets across the most recent N workouts, looked up by exercise
   * name rather than UUID.
   *
   * @param exerciseName human-friendly name (e.g. "Bench Press" or "benchpress")
   * @param lastN maximum number of recent sessions to consider
   * @return list of one-rep max data points for trend plotting
   */
  @Transactional
  public List<OneRmPointDto> getOneRepMaxStats(String exerciseName, int lastN) {
    // 1) Normalize & resolve the name -> Exercise entity
    String normalized = normalize(exerciseName);
    Exercise ex =
        exerciseRepo
            .findByName(normalized)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Unknown exercise \""
                            + exerciseName
                            + "\". Valid options: "
                            + exerciseRepo.findAll().stream()
                                .map(Exercise::getName)
                                .collect(Collectors.joining(", "))));
    UUID exerciseId = ex.getId();

    // 2) Paginate your sets
    var page = PageRequest.of(0, lastN);
    List<WorkoutSet> allSets = workoutRepo.findSetsByExerciseId(exerciseId, page);

    // 3) Retain only the heaviest set per workout
    Map<UUID, WorkoutSet> topSetPerWorkout =
        allSets.stream()
            .collect(
                Collectors.groupingBy(
                    ws -> ws.getWorkout().getId(),
                    Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparing(WorkoutSet::getWeightKg)),
                        Optional::get)));

    // 4) Sort those by date
    List<WorkoutSet> sortedTopSets =
        topSetPerWorkout.values().stream()
            .sorted(Comparator.comparing(ws -> ws.getWorkout().getPerformedOn()))
            .toList();

    // 5) Map to DTOs
    return sortedTopSets.stream()
        .map(
            ws -> {
              double oneRm = ormStrategy.calculate(ws.getWeightKg().doubleValue(), ws.getReps());
              return new OneRmPointDto(
                  ws.getWorkout().getId(), ws.getWorkout().getPerformedOn(), oneRm);
            })
        .toList();
  }

  /**
   * Creates a new workout along with its associated sets and exercises, looking up each Exercise by
   * its human‐friendly name.
   */
  @Transactional
  public UUID createWorkout(WorkoutDto req) {
    // 1) Create the Workout entity
    Workout workout = new Workout();
    workout.setId(UUID.randomUUID());
    workout.setPerformedOn(req.getPerformedOn());

    // 2) Map each incoming DTO → WorkoutSet entity
    List<WorkoutSet> sets =
        req.getSets().stream()
            .map(
                dto -> {
                  // --- lookup by name instead of using dto.getExerciseId()
                  String rawName = dto.getExerciseName();
                  String normalized = NameNormalizer.normalize(rawName);
                  Exercise exercise =
                      exerciseRepo
                          .findByName(normalized)
                          .orElseThrow(
                              () ->
                                  new NoSuchElementException(
                                      "Exercise not found: \"" + rawName + "\""));
                  // ---------------------------------------

                  // build the composite key & entity
                  WorkoutSetId setId = new WorkoutSetId(workout.getId(), dto.getOrder());
                  WorkoutSet ws = new WorkoutSet(setId, exercise, dto.getWeightKg(), dto.getReps());
                  ws.setWorkout(workout);
                  return ws;
                })
            .collect(Collectors.toList());

    // 3) Attach sets and persist
    sets.forEach(workout::addSet);
    workoutRepo.save(workout);

    return workout.getId();
  }

  /**
   * Creates a new exercise.
   *
   * @param dto the exercise details
   * @return the generated UUID for the new exercise
   */
  @Transactional
  public String createExercise(ExerciseDto dto) {
    // Normalize once at creation so repository.findByName(...) will match later
    String normalizedName = NameNormalizer.normalize(dto.getName());
    if (exerciseRepo.existsByName(normalizedName)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "An exercise named '" + dto.getName() + "' already exists");
    }
    Exercise ex = new Exercise(UUID.randomUUID(), normalizedName, dto.getMuscleGroup());
    exerciseRepo.save(ex);
    return ex.getName();
  }

  /**
   * Adds a new set to an existing workout.
   *
   * @param workoutId the target workout's ID
   * @param dto the set to add
   * @throws EntityNotFoundException if the workout is not found
   */
  @Transactional
  public void addSetToWorkout(UUID workoutId, WorkoutSetDto dto) {
    // 1) Load the workout as before
    Workout workout =
        workoutRepo
            .findById(workoutId)
            .orElseThrow(() -> new EntityNotFoundException("Workout not found: " + workoutId));

    // 2) Normalize & lookup the Exercise by name
    String rawName = dto.getExerciseName();
    String normalized = NameNormalizer.normalize(rawName);
    Exercise exercise =
        exerciseRepo
            .findByName(normalized)
            .orElseThrow(
                () -> new EntityNotFoundException("Exercise not found: \"" + rawName + "\""));

    // 3) Build the new set
    WorkoutSetId id = new WorkoutSetId(workoutId, dto.getOrder());
    WorkoutSet set = new WorkoutSet(id, exercise, dto.getWeightKg(), dto.getReps());
    set.setWorkout(workout);

    // 4) Attach & save
    workout.addSet(set);
    workoutRepo.save(workout);
  }

  /**
   * Retrieves a workout and all its sets by ID.
   *
   * @param id the workout ID
   * @return the workout DTO with sets included
   * @throws EntityNotFoundException if not found
   */
  @Transactional(readOnly = true)
  public WorkoutDto getWorkoutWithSets(UUID id) {
    Workout workout =
        workoutRepo
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workout not found: " + id));
    // reuse your single source-of-truth mapping method:
    return mapToDto(workout);
  }

  /**
   * Deletes a workout by its ID.
   *
   * @param id the workout ID
   * @throws EntityNotFoundException if not found
   */
  @Transactional
  public void deleteWorkout(UUID id) {
    if (!workoutRepo.existsById(id)) {
      throw new EntityNotFoundException("Workout not found: " + id);
    }
    workoutRepo.deleteById(id);
  }

  /**
   * Deletes an exercise by its human‐friendly name.
   *
   * @param exerciseName the name of the exercise to delete
   * @throws EntityNotFoundException if no such exercise exists
   */
  @Transactional
  public void deleteExercise(String exerciseName) {
    // 1) normalize & lookup
    String normalized = NameNormalizer.normalize(exerciseName);
    Exercise ex =
        exerciseRepo
            .findByName(normalized)
            .orElseThrow(
                () -> new EntityNotFoundException("Exercise not found: \"" + exerciseName + "\""));

    // 2) delete the entity
    exerciseRepo.delete(ex);
  }

  /**
   * Retrieves all exercises in the system.
   *
   * @return list of exercise DTOs
   */
  @Transactional
  public List<ExerciseDto> getAllExercises() {
    return exerciseRepo.findAll().stream()
        .map(
            e -> {
              ExerciseDto dto = new ExerciseDto();
              dto.setId(e.getId());
              dto.setName(e.getName());
              dto.setMuscleGroup(e.getMuscleGroup());
              return dto;
            })
        .toList();
  }

  /**
   * Retrieves all workouts along with their sets.
   *
   * @return list of full workout DTOs
   */
  @Transactional
  public List<WorkoutDto> getAllWorkouts() {
    return workoutRepo.findAllWithSets().stream().map(this::mapToDto).toList();
  }

  // Maps a Workout entity to its DTO representation with embedded sets
  private WorkoutDto mapToDto(Workout workout) {
    WorkoutDto dto = new WorkoutDto();
    dto.setPerformedOn(workout.getPerformedOn());
    dto.setWorkoutId(workout.getId());

    List<WorkoutSetDto> setDtos =
        workout.getSets().stream()
            .map(
                set -> {
                  WorkoutSetDto s = new WorkoutSetDto();
                  s.setOrder(set.getId().getOrder());
                  s.setWeightKg(set.getWeightKg());
                  s.setReps(set.getReps());
                  s.setExerciseId(set.getExercise().getId());
                  s.setExerciseName(set.getExercise().getName());
                  s.setWorkoutId(set.getWorkout().getId());
                  return s;
                })
            .toList();

    dto.setSets(setDtos);
    return dto;
  }
}
