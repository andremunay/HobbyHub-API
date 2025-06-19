package com.andremunay.hobbyhub.weightlifting.infra;

import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

  /**
   * Fetches the most recent WorkoutSet records for a given exercise, ordered by the workout's
   * performedOn date descending. Used to compute overload trends.
   *
   * @param exerciseId the UUID of the Exercise
   * @param pageable a PageRequest.of(0, lastN) to limit to last N sets
   * @return a list of WorkoutSet entities
   */
  @Query(
      """
      SELECT ws
      FROM WorkoutSet ws
      JOIN ws.workout w
      WHERE ws.exercise.id = :exerciseId
      ORDER BY w.performedOn DESC
      """)
  List<WorkoutSet> findSetsByExerciseId(@Param("exerciseId") UUID exerciseId, Pageable pageable);

  @Query(
      """
    SELECT w FROM Workout w
    LEFT JOIN FETCH w.sets
    """)
  List<Workout> findAllWithSets();
}
