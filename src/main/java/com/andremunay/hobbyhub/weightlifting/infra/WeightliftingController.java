package com.andremunay.hobbyhub.weightlifting.infra;

import com.andremunay.hobbyhub.weightlifting.app.WeightliftingService;
import com.andremunay.hobbyhub.weightlifting.infra.dto.OneRmPoint;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutCreateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<UUID> createWorkout(@Valid @RequestBody WorkoutCreateRequest req) {
    UUID newWorkoutId = weightliftingService.createWorkout(req);
    return ResponseEntity.ok(newWorkoutId);
  }

  /**
   * GET /weightlifting/stats/1rm/{exerciseId}?lastN=3 Returns a List<OneRmPoint> sorted by date
   * ascending.
   */
  @GetMapping("/stats/1rm/{exerciseId}")
  public ResponseEntity<List<OneRmPoint>> getOneRmStats(
      @PathVariable UUID exerciseId, @RequestParam(defaultValue = "3") int lastN) {
    List<OneRmPoint> stats = weightliftingService.getOneRepMaxStats(exerciseId, lastN);
    return ResponseEntity.ok(stats);
  }
}
