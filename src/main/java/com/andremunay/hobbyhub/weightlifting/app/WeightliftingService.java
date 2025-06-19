package com.andremunay.hobbyhub.weightlifting.app;

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

@Service
@RequiredArgsConstructor
public class WeightliftingService {

  private final WorkoutRepository workoutRepo;
  private final ExerciseRepository exerciseRepo;
  private final OneRepMaxStrategy ormStrategy;

  public double calculateOneRepMax(WorkoutSet set) {
    return ormStrategy.calculate(set.getWeightKg().doubleValue(), set.getReps());
  }

  public double computeOverloadTrend(UUID exerciseId, int lastN) {
    Pageable page = PageRequest.of(0, lastN);

    List<WorkoutSet> sets = workoutRepo.findSetsByExerciseId(exerciseId, page);

    List<WorkoutSet> topSets =
        sets.stream()
            .sorted(Comparator.comparing(ws -> ws.getWorkout().getPerformedOn()))
            .collect(
                Collectors.groupingBy(
                    ws -> ws.getWorkout().getId(),
                    Collectors.maxBy(Comparator.comparing(WorkoutSet::getWeightKg))))
            .values()
            .stream()
            .map(Optional::get)
            .limit(lastN)
            .toList();

    SimpleRegression regression = new SimpleRegression();
    for (int i = 0; i < topSets.size(); i++) {
      regression.addData(i, topSets.get(i).getWeightKg().doubleValue());
    }

    return regression.getSlope();
  }

  /** Given an exerciseId and lastN, compute a list of OneRmPointDto sorted by date ascending. */
  @Transactional(readOnly = true)
  public List<OneRmPointDto> getOneRepMaxStats(UUID exerciseId, int lastN) {
    // 1) wrap lastN into a Pageable
    var page = PageRequest.of(0, lastN);

    // 2) fetch WorkoutSet rows (newest first)
    List<WorkoutSet> allSets = workoutRepo.findSetsByExerciseId(exerciseId, page);

    // 3) group by Workout ID → pick the set with max weight per workout
    Map<UUID, WorkoutSet> topSetPerWorkout =
        allSets.stream()
            .collect(
                Collectors.groupingBy(
                    ws -> ws.getWorkout().getId(),
                    Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparing(WorkoutSet::getWeightKg)),
                        Optional::get)));

    // 4) Extract and sort by performedOn ascending
    List<WorkoutSet> sortedTopSets =
        topSetPerWorkout.values().stream()
            .sorted(Comparator.comparing(ws -> ws.getWorkout().getPerformedOn()))
            .toList();

    // 5) Map each to OneRmPointDto DTO
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
   * Creates a new Workout (and its nested sets) from the given request. Returns the generated
   * Workout UUID.
   */
  @Transactional
  public UUID createWorkout(WorkoutDto req) {
    // 1) Map top‐level fields
    Workout workout = new Workout();
    workout.setId(UUID.randomUUID());
    workout.setPerformedOn(req.getPerformedOn());

    // 2) For each nested set DTO:
    List<WorkoutSet> sets =
        req.getSets().stream()
            .map(
                dto -> {
                  // 2a) Lookup Exercise entity (throws if not found)
                  UUID exId = dto.getExerciseId();
                  Exercise exercise =
                      exerciseRepo
                          .findById(exId)
                          .orElseThrow(
                              () -> new NoSuchElementException("Exercise not found: " + exId));

                  // 2b) Build WorkoutSetId with workoutId + order
                  WorkoutSetId setId = new WorkoutSetId(workout.getId(), dto.getOrder());

                  // 2c) Construct WorkoutSet entity
                  WorkoutSet set =
                      new WorkoutSet(setId, exercise, dto.getWeightKg(), dto.getReps());

                  // 2d) Set the bidirectional link to parent Workout
                  set.setWorkout(workout);

                  return set;
                })
            .collect(Collectors.toList());

    // 3) Attach sets to workout (cascade = ALL)
    sets.forEach(workout::addSet);

    // 4) Save Workout (persist sets as well)
    workoutRepo.save(workout);

    return workout.getId();
  }

  @Transactional
  public UUID createExercise(ExerciseDto dto) {
    Exercise ex = new Exercise(UUID.randomUUID(), dto.getName(), dto.getMuscleGroup());
    exerciseRepo.save(ex);
    return ex.getId();
  }

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

  @Transactional
  public void deleteWorkout(UUID id) {
    if (!workoutRepo.existsById(id)) {
      throw new EntityNotFoundException("Workout not found: " + id);
    }
    workoutRepo.deleteById(id);
  }

  public void deleteExercise(UUID id) {
    exerciseRepo.deleteById(id);
  }

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

  public List<WorkoutDto> getAllWorkouts() {
    return workoutRepo.findAllWithSets().stream().map(this::mapToDto).toList();
  }

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
