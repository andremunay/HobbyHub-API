package com.andremunay.hobbyhub.weightlifting.app;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSetId;
import com.andremunay.hobbyhub.weightlifting.infra.ExerciseRepository;
import com.andremunay.hobbyhub.weightlifting.infra.WorkoutRepository;
import com.andremunay.hobbyhub.weightlifting.infra.dto.OneRmPoint;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutCreateRequest;
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

  /** Given an exerciseId and lastN, compute a list of OneRmPoint sorted by date ascending. */
  @Transactional(readOnly = true)
  public List<OneRmPoint> getOneRepMaxStats(UUID exerciseId, int lastN) {
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
            .collect(Collectors.toList());

    // 5) Map each to OneRmPoint DTO
    return sortedTopSets.stream()
        .map(
            ws -> {
              BigDecimal weight = ws.getWeightKg();
              int reps = ws.getReps();
              double oneRm = ormStrategy.calculate(weight.doubleValue(), reps);
              return new OneRmPoint(
                  ws.getWorkout().getId(), ws.getWorkout().getPerformedOn(), oneRm);
            })
        .collect(Collectors.toList());
  }

  /**
   * Creates a new Workout (and its nested sets) from the given request. Returns the generated
   * Workout UUID.
   */
  @Transactional
  public UUID createWorkout(WorkoutCreateRequest req) {
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
                  UUID exId = UUID.fromString(dto.getExerciseId());
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
}
