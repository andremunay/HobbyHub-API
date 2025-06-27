package com.andremunay.hobbyhub.weightlifting.infra;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andremunay.hobbyhub.weightlifting.app.WeightliftingService;
import com.andremunay.hobbyhub.weightlifting.infra.dto.ExerciseDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.OneRmPointDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutSetDto;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link WeightliftingController}, verifying REST endpoints, input handling,
 * delegation to service layer, and expected HTTP responses.
 */
@ExtendWith(MockitoExtension.class)
class WeightliftingControllerTest {

  @Mock private WeightliftingService weightliftingService;
  @InjectMocks private WeightliftingController weightliftingController;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(weightliftingController).build();
  }

  /** Verifies that GET /exercises returns an empty list when no exercises exist. */
  @Test
  void getAllExercises() throws Exception {
    BDDMockito.given(weightliftingService.getAllExercises()).willReturn(Collections.emptyList());

    mvc.perform(get("/weightlifting/exercises"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** Verifies that GET /workouts returns an empty list when no workouts exist. */
  @Test
  void getAllWorkouts() throws Exception {
    BDDMockito.given(weightliftingService.getAllWorkouts()).willReturn(Collections.emptyList());

    mvc.perform(get("/weightlifting/workouts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** Tests creation of a workout and verifies that the service returns the correct ID. */
  @Test
  void createWorkout() throws Exception {
    UUID id = UUID.randomUUID();
    BDDMockito.given(weightliftingService.createWorkout(Mockito.any(WorkoutDto.class)))
        .willReturn(id);

    String payload =
        "{\"performedOn\":\"2025-01-01\",\"sets\":["
            + "{\"exerciseName\":\"benchpress\",\"weightKg\":10.0,\"reps\":1,\"order\":0}"
            + "]}";

    mvc.perform(
            post("/weightlifting/workouts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(id.toString()));

    Mockito.verify(weightliftingService).createWorkout(Mockito.any(WorkoutDto.class));
  }

  /** Verifies that one-rep max stats are fetched correctly and returned as JSON. */
  @Test
  void getOneRmStats_byName() throws Exception {
    String exerciseName = "benchpress";
    OneRmPointDto point = new OneRmPointDto();
    point.setDate(LocalDate.now());
    point.setOneRepMax(100.0);

    // stub the service to expect (exerciseName, lastN)
    BDDMockito.given(weightliftingService.getOneRepMaxStats(exerciseName, 3))
        .willReturn(List.of(point));

    mvc.perform(
            get("/weightlifting/stats/1rm").param("exerciseName", exerciseName).param("lastN", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].oneRepMax").value(100.0));

    // verify the new signature was invoked
    Mockito.verify(weightliftingService).getOneRepMaxStats(exerciseName, 3);
  }

  /** Tests creation of an exercise and confirms the returned name. */
  @Test
  void createExercise() throws Exception {
    String name = "squat";
    BDDMockito.given(weightliftingService.createExercise(Mockito.any(ExerciseDto.class)))
        .willReturn(name);

    String payload = "{\"name\":\"Squat\",\"muscleGroup\":\"Legs\"}";

    mvc.perform(
            post("/weightlifting/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(name));

    Mockito.verify(weightliftingService).createExercise(Mockito.any(ExerciseDto.class));
  }

  /** Verifies that a new set can be added to an existing workout. */
  @Test
  void addSet() throws Exception {
    UUID workoutId = UUID.randomUUID();
    BDDMockito.willDoNothing()
        .given(weightliftingService)
        .addSetToWorkout(eq(workoutId), Mockito.any(WorkoutSetDto.class));

    // now send exerciseName instead of exerciseId
    String payload = "{\"exerciseName\":\"benchpress\",\"weightKg\":100.0,\"reps\":5,\"order\":1}";

    mvc.perform(
            post("/weightlifting/workouts/{workoutId}/sets", workoutId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    Mockito.verify(weightliftingService)
        .addSetToWorkout(eq(workoutId), Mockito.any(WorkoutSetDto.class));
  }

  /** Retrieves a specific workout and asserts correct mapping to the DTO. */
  @Test
  void getWorkout() throws Exception {
    UUID id = UUID.randomUUID();
    WorkoutDto dto = new WorkoutDto();
    dto.setWorkoutId(id);
    dto.setPerformedOn(LocalDate.now());
    dto.setSets(Collections.emptyList());
    BDDMockito.given(weightliftingService.getWorkoutWithSets(id)).willReturn(dto);

    mvc.perform(get("/weightlifting/workouts/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workoutId").value(id.toString()));

    Mockito.verify(weightliftingService).getWorkoutWithSets(id);
  }

  /** Ensures a workout can be deleted by ID. */
  @Test
  void deleteWorkout() throws Exception {
    UUID id = UUID.randomUUID();
    BDDMockito.willDoNothing().given(weightliftingService).deleteWorkout(id);

    mvc.perform(delete("/weightlifting/workouts/{id}", id)).andExpect(status().isNoContent());

    Mockito.verify(weightliftingService).deleteWorkout(id);
  }

  /** Ensures an exercise can be deleted by ID. */
  @Test
  void deleteExercise_byName() throws Exception {
    String exerciseName = "benchpress";
    BDDMockito.willDoNothing().given(weightliftingService).deleteExercise(exerciseName);

    mvc.perform(delete("/weightlifting/exercises").param("exerciseName", exerciseName))
        .andExpect(status().isNoContent());

    Mockito.verify(weightliftingService).deleteExercise(exerciseName);
  }

  /** Returns HTTP 400 when an invalid UUID is passed to GET /workouts/{id}. */
  @Test
  void getWorkout_invalidUuid_returnsBadRequest() throws Exception {
    mvc.perform(get("/weightlifting/workouts/invalid-uuid")).andExpect(status().isBadRequest());
  }

  /** Computes the overload trend for an exercise and returns the slope value. */
  @Test
  void getOverloadTrend_returnsSlope_byName() throws Exception {
    String exerciseName = "benchpress";
    BDDMockito.given(weightliftingService.computeOverloadTrend(eq(exerciseName), eq(5)))
        .willReturn(-12.34);

    mvc.perform(
            get("/weightlifting/stats/trend")
                .param("exerciseName", exerciseName)
                .param("lastN", "5")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string("-12.34"));

    Mockito.verify(weightliftingService).computeOverloadTrend(exerciseName, 5);
  }

  /** Returns HTTP 400 when a malformed UUID is passed to /trend endpoint. */
  @Test
  void getOverloadTrend_missingExerciseName_returnsBadRequest() throws Exception {
    mvc.perform(get("/weightlifting/stats/trend").param("lastN", "5"))
        .andExpect(status().isBadRequest());
  }
}
