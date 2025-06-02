package com.andremunay.hobbyhub.weightlifting.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSetId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(com.andremunay.hobbyhub.TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class WeightliftingIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ExerciseRepository exerciseRepo;

  @Autowired private WorkoutRepository workoutRepo;

  private UUID exerciseId;

  @BeforeEach
  void setUp() {
    // 1) Create one Exercise in DB
    Exercise exercise = new Exercise(UUID.randomUUID(), "Bench Press", "Chest");
    exerciseRepo.save(exercise);
    this.exerciseId = exercise.getId();

    // 2) Create 3 Workouts on consecutive days, each with one WorkoutSet for that Exercise
    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 2));
    Workout w3 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 3));

    // Persist these workouts first so that they have an ID
    workoutRepo.saveAll(List.of(w1, w2, w3));

    // Now add one WorkoutSet to each, with strictly increasing weight
    WorkoutSet s1 =
        new WorkoutSet(new WorkoutSetId(w1.getId(), 1), exercise, BigDecimal.valueOf(50), 5);
    s1.setWorkout(w1);

    WorkoutSet s2 =
        new WorkoutSet(new WorkoutSetId(w2.getId(), 1), exercise, BigDecimal.valueOf(60), 5);
    s2.setWorkout(w2);

    WorkoutSet s3 =
        new WorkoutSet(new WorkoutSetId(w3.getId(), 1), exercise, BigDecimal.valueOf(70), 5);
    s3.setWorkout(w3);

    // Save all sets by saving the owning Workout (cascade = ALL)
    w1.addSet(s1);
    w2.addSet(s2);
    w3.addSet(s3);

    // Persist changes
    workoutRepo.saveAll(List.of(w1, w2, w3));
  }

  @Test
  void getOneRepMaxStats_returnsAscendingDatesAndCorrectValues() {
    // Hit the endpoint: GET /weightlifting/stats/1rm/{exerciseId}?lastN=3
    String url = "http://localhost:" + port + "/weightlifting/stats/1rm/" + exerciseId + "?lastN=3";

    ResponseEntity<List<WeightStat>> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<WeightStat> body = response.getBody();
    assertThat(body).isNotNull().hasSize(3);

    // The JSON should be sorted by date ascending: [2025-05-01, 2025-05-02, 2025-05-03]
    assertThat(body.get(0).getDate()).isEqualTo(LocalDate.of(2025, 5, 1));
    assertThat(body.get(1).getDate()).isEqualTo(LocalDate.of(2025, 5, 2));
    assertThat(body.get(2).getDate()).isEqualTo(LocalDate.of(2025, 5, 3));

    // Epley 1RM formula: for weight=50, reps=5 → 50 * (1 + 5/30) = 58.3333…
    // for 60 & 5 → 60 * (1 + 5/30) = 69.9999…, for 70 & 5 → 81.6666…
    double rm1 = body.get(0).getOneRepMax();
    double rm2 = body.get(1).getOneRepMax();
    double rm3 = body.get(2).getOneRepMax();

    assertThat(rm1).isCloseTo(58.3333, within(1e-3));
    assertThat(rm2).isCloseTo(70.0000, within(1e-3));
    assertThat(rm3).isCloseTo(81.6667, within(1e-3));
  }

  // Helper class to map JSON response (the field names must match WeightStatDto)
  @Getter
  @Setter
  @NoArgsConstructor
  public static class WeightStat {
    private UUID workoutId;
    private LocalDate date;
    private double oneRepMax;
  }
}
