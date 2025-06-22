package com.andremunay.hobbyhub.weightlifting.app;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
   * Estimates the slope of progressive overload over the most recent N workouts.
   *
   * <p>Measures performance trend based on top sets per workout using linear regression.
   *
   * @param exerciseId the exercise to analyze
   * @param lastN number of most recent workouts to include
   * @return positive/negative slope of weight progression (0.0 if insufficient data)
   */
  public double computeOverloadTrend(UUID exerciseId, int lastN) {
    Pageable page = PageRequest.of(0, lastN);

    List<WorkoutSet> sets = workoutRepo.findSetsByExerciseId(exerciseId, page);

    // Extract highest-weight set per workout
    List<WorkoutSet> topSets =
        sets.stream()
            .sorted(comparing(ws -> ws.getWorkout().getPerformedOn()))
            .collect(
                groupingBy(
                    ws -> ws.getWorkout().getId(), maxBy(comparing(WorkoutSet::getWeightKg))))
            .values()
            .stream()
            .map(Optional::get)
            .limit(lastN)
            .sorted(comparing(ws -> ws.getWorkout().getPerformedOn()))
            .toList();

    if (topSets.size() < 2) {
      return 0.0;
    }

    // Perform simple linear regression on weights over time
    SimpleRegression regression = new SimpleRegression();
    for (int i = 0; i < topSets.size(); i++) {
      regression.addData(i, topSets.get(i).getWeightKg().doubleValue());
    }

    return regression.getSlope();
  }

  /**
   * Calculates estimated 1RM for top sets across the most recent N workouts.
   *
   * @param exerciseId exercise identifier
   * @param lastN maximum number of recent sessions to consider
   * @return list of one-rep max data points for trend plotting
   */
  @Transactional(readOnly = true)
  public List<OneRmPointDto> getOneRepMaxStats(UUID exerciseId, int lastN) {
    var page = PageRequest.of(0, lastN);

    List<WorkoutSet> allSets = workoutRepo.findSetsByExerciseId(exerciseId, page);

    // Retain the top-weight set per workout
    Map<UUID, WorkoutSet> topSetPerWorkout =
        allSets.stream()
            .collect(
                Collectors.groupingBy(
                    ws -> ws.getWorkout().getId(),
                    Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparing(WorkoutSet::getWeightKg)),
                        Optional::get)));

    List<WorkoutSet> sortedTopSets =
        topSetPerWorkout.values().stream()
            .sorted(Comparator.comparing(ws -> ws.getWorkout().getPerformedOn()))
            .toList();

    return sortedTopSets.stream()
        .map(
            ws -> {
              BigDecimal weight = ws.getWeightKg();
              int reps = ws.getReps();
              double oneRm = ormStrategy.calculate(weight.doubleValue(), reps);
              return new OneRmPointDto(
                  ws.getWorkout().getId(), ws.getWorkout().getPerformedOn(), oneRm);
            })
        .toList();
  }

  /**
   * Creates a new workout along with its associated sets and exercises.
   *
   * @param req the workout data submitted by the client
   * @return the ID of the newly created workout
   * @throws NoSuchElementException if any exercise in the set is missing
   */
  @Transactional
  public UUID createWorkout(WorkoutDto req) {
    Workout workout = new Workout();
    workout.setId(UUID.randomUUID());
    workout.setPerformedOn(req.getPerformedOn());

    List<WorkoutSet> sets =
        req.getSets().stream()
            .map(
                dto -> {
                  UUID exId = dto.getExerciseId();
                  Exercise exercise =
                      exerciseRepo
                          .findById(exId)
                          .orElseThrow(
                              () -> new NoSuchElementException("Exercise not found: " + exId));

                  WorkoutSetId setId = new WorkoutSetId(workout.getId(), dto.getOrder());

                  WorkoutSet set =
                      new WorkoutSet(setId, exercise, dto.getWeightKg(), dto.getReps());

                  set.setWorkout(workout);

                  return set;
                })
            .collect(Collectors.toList());

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
  public UUID createExercise(ExerciseDto dto) {
    Exercise ex = new Exercise(UUID.randomUUID(), dto.getName(), dto.getMuscleGroup());
    exerciseRepo.save(ex);
    return ex.getId();
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
    Workout workout =
        workoutRepo
            .findById(workoutId)
            .orElseThrow(() -> new EntityNotFoundException("Workout not found: " + workoutId));

    UUID exerciseId = dto.getExerciseId();
    Exercise exercise = exerciseRepo.getReferenceById(exerciseId);

    WorkoutSetId id = new WorkoutSetId(workoutId, dto.getOrder());

    WorkoutSet set = new WorkoutSet(id, exercise, dto.getWeightKg(), dto.getReps());

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

    List<WorkoutSetDto> setDtos =
        workout.getSets().stream()
            .map(
                set -> {
                  WorkoutSetDto dto = new WorkoutSetDto();
                  dto.setExerciseId(set.getExercise().getId());
                  dto.setWeightKg(set.getWeightKg());
                  dto.setReps(set.getReps());
                  dto.setOrder(set.getId().getOrder());
                  return dto;
                })
            .toList();

    WorkoutDto response = new WorkoutDto();
    response.setPerformedOn(workout.getPerformedOn());
    response.setSets(setDtos);

    return response;
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
   * Deletes an exercise by ID.
   *
   * @param id the exercise ID
   */
  public void deleteExercise(UUID id) {
    exerciseRepo.deleteById(id);
  }

  /**
   * Retrieves all exercises in the system.
   *
   * @return list of exercise DTOs
   */
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
  public List<WorkoutDto> getAllWorkouts() {
    return workoutRepo.findAllWithSets().stream().map(this::mapToDto).toList();
  }

  // Maps a Workout entity to its DTO representation with embedded sets
  private WorkoutDto mapToDto(Workout workout) {
    WorkoutDto dto = new WorkoutDto();
    dto.setPerformedOn(workout.getPerformedOn());

    List<WorkoutSetDto> setDtos =
        workout.getSets().stream()
            .map(
                set -> {
                  WorkoutSetDto s = new WorkoutSetDto();
                  s.setOrder(set.getId().getOrder());
                  s.setWeightKg(set.getWeightKg());
                  s.setReps(set.getReps());
                  s.setExerciseId(set.getExercise().getId());
                  return s;
                })
            .toList();

    dto.setSets(setDtos);
    return dto;
  }
}
