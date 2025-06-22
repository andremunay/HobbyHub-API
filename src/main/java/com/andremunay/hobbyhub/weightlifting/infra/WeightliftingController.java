package com.andremunay.hobbyhub.weightlifting.infra;

import com.andremunay.hobbyhub.weightlifting.app.WeightliftingService;
import com.andremunay.hobbyhub.weightlifting.infra.dto.ExerciseDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.OneRmPointDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutSetDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing weightlifting data, including workouts, exercises, and strength
 * analytics such as one-rep max tracking and overload trend analysis.
 */
@RestController
@RequestMapping("/weightlifting")
@RequiredArgsConstructor
public class WeightliftingController {

  private final WeightliftingService weightliftingService;

  /**
   * Retrieves all workouts along with their associated sets.
   *
   * @return HTTP 200 with list of full workout DTOs
   */
  @GetMapping("/workouts")
  public ResponseEntity<List<WorkoutDto>> getAllWorkouts() {
    List<WorkoutDto> workouts = weightliftingService.getAllWorkouts();
    return ResponseEntity.ok(workouts);
  }

  /**
   * Retrieves all available exercises.
   *
   * @return HTTP 200 with list of exercises
   */
  @GetMapping("/exercises")
  public ResponseEntity<List<ExerciseDto>> getAllExercises() {
    List<ExerciseDto> exercises = weightliftingService.getAllExercises();
    return ResponseEntity.ok(exercises);
  }

  /**
   * Retrieves a specific workout and its sets by ID.
   *
   * @param id the UUID of the workout
   * @return HTTP 200 with workout details
   */
  @GetMapping("/workouts/{id}")
  public ResponseEntity<WorkoutDto> getWorkout(@PathVariable UUID id) {
    return ResponseEntity.ok(weightliftingService.getWorkoutWithSets(id));
  }

  /**
   * Retrieves one-rep max statistics for a given exercise over the last N sessions.
   *
   * @param exerciseId exercise identifier
   * @param lastN number of recent workouts to include (default = 3)
   * @return HTTP 200 with list of 1RM data points
   */
  @GetMapping("/stats/1rm/{exerciseId}")
  public ResponseEntity<List<OneRmPointDto>> getOneRmStats(
      @PathVariable UUID exerciseId, @RequestParam(defaultValue = "3") int lastN) {
    List<OneRmPointDto> stats = weightliftingService.getOneRepMaxStats(exerciseId, lastN);
    return ResponseEntity.ok(stats);
  }

  /**
   * Computes the linear regression slope of weight lifted over the last N sessions.
   *
   * <p>Used to estimate progressive overload trends.
   *
   * @param exerciseId exercise to analyze
   * @param lastN number of sessions to evaluate (default = 10)
   * @return HTTP 200 with numeric slope value
   */
  @GetMapping("/exercises/{exerciseId}/trend")
  public ResponseEntity<Double> getOverloadTrend(
      @PathVariable("exerciseId") UUID exerciseId,
      @RequestParam(name = "lastN", defaultValue = "10") int lastN) {
    double slope = weightliftingService.computeOverloadTrend(exerciseId, lastN);
    return ResponseEntity.ok(slope);
  }

  /**
   * Creates a new workout session and associated sets.
   *
   * @param req validated workout data
   * @return HTTP 200 with generated workout ID
   */
  @PostMapping("/workouts")
  public ResponseEntity<UUID> createWorkout(@Valid @RequestBody WorkoutDto req) {
    UUID newWorkoutId = weightliftingService.createWorkout(req);
    return ResponseEntity.ok(newWorkoutId);
  }

  /**
   * Creates a new exercise definition.
   *
   * @param dto validated exercise details
   * @return HTTP 200 with generated exercise ID
   */
  @PostMapping("/exercises")
  public ResponseEntity<UUID> createExercise(@Valid @RequestBody ExerciseDto dto) {
    UUID id = weightliftingService.createExercise(dto);
    return ResponseEntity.ok(id);
  }

  /**
   * Adds a set to an existing workout.
   *
   * @param workoutId the workout to add the set to
   * @param dto the set to add
   * @return HTTP 200 on success
   */
  @PostMapping("workouts/{workoutId}/sets")
  public ResponseEntity<Void> addSet(
      @PathVariable UUID workoutId, @Valid @RequestBody WorkoutSetDto dto) {
    weightliftingService.addSetToWorkout(workoutId, dto);
    return ResponseEntity.ok().build();
  }

  /**
   * Deletes a workout by ID.
   *
   * @param id the workout ID
   * @return HTTP 204 if successfully deleted
   */
  @DeleteMapping("/workouts/{id}")
  public ResponseEntity<Void> deleteWorkout(@PathVariable UUID id) {
    weightliftingService.deleteWorkout(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Deletes an exercise by ID.
   *
   * @param id the exercise ID
   * @return HTTP 204 if successfully deleted
   */
  @DeleteMapping("/exercises/{id}")
  public ResponseEntity<Void> deleteExercise(@PathVariable UUID id) {
    weightliftingService.deleteExercise(id);
    return ResponseEntity.noContent().build();
  }
}
