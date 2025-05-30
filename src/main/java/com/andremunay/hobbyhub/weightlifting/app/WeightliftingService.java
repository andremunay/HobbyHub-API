package com.andremunay.hobbyhub.weightlifting.app;

import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import com.andremunay.hobbyhub.weightlifting.infra.WorkoutRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeightliftingService {

  private final WorkoutRepository workoutRepo;
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
}
