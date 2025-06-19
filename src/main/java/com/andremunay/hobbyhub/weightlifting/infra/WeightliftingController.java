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

@RestController
@RequestMapping("/weightlifting")
@RequiredArgsConstructor
public class WeightliftingController {

  private final WeightliftingService weightliftingService;

  /**
   * POST /weightlifting/workouts Accepts a JSON body with performedOn and nested sets, returns the
   * newly created Workout's UUID.
   */
  @PostMapping("/workouts")
  public ResponseEntity<UUID> createWorkout(@Valid @RequestBody WorkoutDto req) {
    UUID newWorkoutId = weightliftingService.createWorkout(req);
    return ResponseEntity.ok(newWorkoutId);
  }

  /**
   * GET /weightlifting/stats/1rm/{exerciseId}?lastN=3 Returns a List<OneRmPointDto> sorted by date
   * ascending.
   */
  @GetMapping("/stats/1rm/{exerciseId}")
  public ResponseEntity<List<OneRmPointDto>> getOneRmStats(
      @PathVariable UUID exerciseId, @RequestParam(defaultValue = "3") int lastN) {
    List<OneRmPointDto> stats = weightliftingService.getOneRepMaxStats(exerciseId, lastN);
    return ResponseEntity.ok(stats);
  }

  @PostMapping("/exercises")
  public ResponseEntity<UUID> createExercise(@Valid @RequestBody ExerciseDto dto) {
    UUID id = weightliftingService.createExercise(dto);
    return ResponseEntity.ok(id);
  }

  @PostMapping("workouts/{workoutId}/sets")
  public ResponseEntity<Void> addSet(
      @PathVariable UUID workoutId, @Valid @RequestBody WorkoutSetDto dto) {
    weightliftingService.addSetToWorkout(workoutId, dto);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/workouts/{id}")
  public ResponseEntity<WorkoutDto> getWorkout(@PathVariable UUID id) {
    return ResponseEntity.ok(weightliftingService.getWorkoutWithSets(id));
  }

  @DeleteMapping("/workouts/{id}")
  public ResponseEntity<Void> deleteWorkout(@PathVariable UUID id) {
    weightliftingService.deleteWorkout(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/exercises/{id}")
  public ResponseEntity<Void> deleteExercise(@PathVariable UUID id) {
    weightliftingService.deleteExercise(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/exercises")
  public ResponseEntity<List<ExerciseDto>> getAllExercises() {
    List<ExerciseDto> exercises = weightliftingService.getAllExercises();
    return ResponseEntity.ok(exercises);
  }

  @GetMapping("/workouts")
  public ResponseEntity<List<WorkoutDto>> getAllWorkouts() {
    List<WorkoutDto> workouts = weightliftingService.getAllWorkouts();
    return ResponseEntity.ok(workouts);
  }
}
