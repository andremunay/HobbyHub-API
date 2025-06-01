package com.andremunay.hobbyhub.weightlifting.infra;

import com.andremunay.hobbyhub.weightlifting.app.WeightStatDto;
import com.andremunay.hobbyhub.weightlifting.app.WeightliftingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class WeightliftingController {

  private final WeightliftingService weightliftingService;

  @GetMapping("/1rm/{exerciseId}")
  public ResponseEntity<List<WeightStatDto>> getOneRepMaxStats(
      @PathVariable UUID exerciseId, @RequestParam(defaultValue = "3") int lastN) {
    List<WeightStatDto> stats = weightliftingService.getOneRepMaxStats(exerciseId, lastN);
    return ResponseEntity.ok(stats);
  }
}
