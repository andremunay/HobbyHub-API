package com.andremunay.hobbyhub.weightlifting.infra;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for accessing and querying {@link Workout} entities.
 *
 * <p>Includes custom methods for retrieving sets by exercise and eagerly loading workout data.
 */
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {
  /**
   * Fetches the most recent {@link WorkoutSet} records for a given exercise, ordered by the
   * associated workout's {@code performedOn} date in descending order.
   *
   * <p>Used to compute overload trends and recent performance metrics.
   *
   * @param exerciseId the UUID of the {@link Exercise}
   * @param pageable a PageRequest limiting the number of results
   * @return list of {@link WorkoutSet} entities
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

  /**
   * Retrieves all workouts with their associated sets eagerly fetched.
   *
   * <p>Avoids the N+1 select problem when sets are needed alongside workout data.
   *
   * @return list of workouts with sets populated
   */
  @Query(
      """
    SELECT w FROM Workout w
    LEFT JOIN FETCH w.sets
    """)
  List<Workout> findAllWithSets();
}
