package com.andremunay.hobbyhub.weightlifting.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import com.andremunay.hobbyhub.weightlifting.domain.Workout;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSet;
import com.andremunay.hobbyhub.weightlifting.domain.WorkoutSetId;
import com.andremunay.hobbyhub.weightlifting.infra.dto.ExerciseDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutSetDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(com.andremunay.hobbyhub.TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class WeightliftingIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ExerciseRepository exerciseRepo;
  @Autowired private WorkoutRepository workoutRepo;
  @Autowired private ObjectMapper objectMapper;

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

  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void getOneRepMaxStats_returnsAscendingDatesAndCorrectValues() throws Exception {
    // Hit the endpoint: GET /weightlifting/stats/1rm/{exerciseId}?lastN=3
    String url = "/weightlifting/stats/1rm/" + exerciseId + "?lastN=3";

    MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();

    String json = result.getResponse().getContentAsString();
    List<WeightStat> stats = objectMapper.readValue(json, new TypeReference<>() {});

    assertThat(stats).isNotNull().hasSize(3);

    // The JSON should be sorted by date ascending: [2025-05-01, 2025-05-02, 2025-05-03]
    assertThat(stats.get(0).getDate()).isEqualTo(LocalDate.of(2025, 5, 1));
    assertThat(stats.get(1).getDate()).isEqualTo(LocalDate.of(2025, 5, 2));
    assertThat(stats.get(2).getDate()).isEqualTo(LocalDate.of(2025, 5, 3));

    // Epley 1RM formula: for weight=50, reps=5 → 50 * (1 + 5/30) = 58.3333…
    // for 60 & 5 → 60 * (1 + 5/30) = 69.9999…, for 70 & 5 → 81.6666…
    double rm1 = stats.get(0).getOneRepMax();
    double rm2 = stats.get(1).getOneRepMax();
    double rm3 = stats.get(2).getOneRepMax();

    assertThat(rm1).isCloseTo(58.3333, within(1e-3));
    assertThat(rm2).isCloseTo(70.0000, within(1e-3));
    assertThat(rm3).isCloseTo(81.6667, within(1e-3));
  }

  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void createWorkout_thenAddSet_succeeds() throws Exception {
    // Step 1: Build workout with 1 initial set
    WorkoutDto workoutRequest = new WorkoutDto();
    workoutRequest.setPerformedOn(LocalDate.of(2025, 6, 18));

    WorkoutSetDto initialSet = new WorkoutSetDto();
    initialSet.setOrder(1);
    initialSet.setWeightKg(BigDecimal.valueOf(50));
    initialSet.setReps(5);
    initialSet.setExerciseId(exerciseId);

    workoutRequest.setSets(List.of(initialSet));

    // Serialize and post workout
    String workoutJson = objectMapper.writeValueAsString(workoutRequest);
    MvcResult result =
        mockMvc
            .perform(
                post("/weightlifting/workouts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(workoutJson))
            .andExpect(status().isOk())
            .andReturn();

    String raw = result.getResponse().getContentAsString().replace("\"", "");
    UUID workoutId = UUID.fromString(raw);

    // Step 2: Add a second set to the workout
    WorkoutSetDto additionalSet = new WorkoutSetDto();
    additionalSet.setOrder(2);
    additionalSet.setWeightKg(BigDecimal.valueOf(55));
    additionalSet.setReps(4);
    additionalSet.setExerciseId(exerciseId);

    String setJson = objectMapper.writeValueAsString(additionalSet);

    mockMvc
        .perform(
            post("/weightlifting/workouts/" + workoutId + "/sets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(setJson))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void createExercise_succeeds() throws Exception {
    // Step 1: build request
    ExerciseDto req = new ExerciseDto();
    req.setName("Squat");
    req.setMuscleGroup("Legs");

    String json = objectMapper.writeValueAsString(req);

    // Step 2: POST /weightlifting/exercises
    MvcResult mvc =
        mockMvc
            .perform(
                post("/weightlifting/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isOk())
            .andReturn();

    // Step 3: parse ID and assert persistence
    UUID id = UUID.fromString(mvc.getResponse().getContentAsString().replace("\"", ""));
    assertThat(exerciseRepo.findById(id)).isPresent();
  }

  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void getWorkout_withSets_returnsCorrectDto() throws Exception {
    // Arrange: reuse create+addSet flow to get a workoutId
    WorkoutDto workoutReq = new WorkoutDto();
    workoutReq.setPerformedOn(LocalDate.of(2025, 6, 18));
    WorkoutSetDto set = new WorkoutSetDto();
    set.setOrder(1);
    set.setWeightKg(BigDecimal.valueOf(80));
    set.setReps(3);
    set.setExerciseId(exerciseId);
    workoutReq.setSets(List.of(set));
    String wJson = objectMapper.writeValueAsString(workoutReq);

    String rawId =
        mockMvc
            .perform(
                post("/weightlifting/workouts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(wJson))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("\"", "");
    UUID workoutId = UUID.fromString(rawId);

    // Act: GET the workout
    MvcResult getRes =
        mockMvc
            .perform(get("/weightlifting/workouts/" + workoutId))
            .andExpect(status().isOk())
            .andReturn();

    // Assert: deserialize and verify
    WorkoutDto dto =
        objectMapper.readValue(
            getRes.getResponse().getContentAsString(), new TypeReference<WorkoutDto>() {});

    assertThat(dto.getPerformedOn()).isEqualTo(LocalDate.of(2025, 6, 18));
    assertThat(dto.getSets()).hasSize(1);
    WorkoutSetDto retSet = dto.getSets().get(0);
    assertThat(retSet.getOrder()).isEqualTo(1);
    assertThat(retSet.getWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(80));
    assertThat(retSet.getReps()).isEqualTo(3);
  }

  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void deleteWorkout_succeeds() throws Exception {
    // Arrange: create a workout
    WorkoutDto wr = new WorkoutDto();
    wr.setPerformedOn(LocalDate.of(2025, 6, 19));
    WorkoutSetDto s = new WorkoutSetDto();
    s.setOrder(1);
    s.setWeightKg(BigDecimal.valueOf(65));
    s.setReps(8);
    s.setExerciseId(exerciseId);
    wr.setSets(List.of(s));
    String json = objectMapper.writeValueAsString(wr);

    String idRaw =
        mockMvc
            .perform(
                post("/weightlifting/workouts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("\"", "");
    UUID wid = UUID.fromString(idRaw);

    // Act: DELETE /weightlifting/workouts/{id}
    mockMvc.perform(delete("/weightlifting/workouts/" + wid)).andExpect(status().isNoContent());

    // Assert: no longer present
    assertThat(workoutRepo.findById(wid)).isEmpty();
  }

  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void deleteExercise_succeeds() throws Exception {
    // Arrange: create an exercise
    ExerciseDto ex = new ExerciseDto();
    ex.setName("Deadlift");
    ex.setMuscleGroup("Back");
    String exJson = objectMapper.writeValueAsString(ex);

    String exIdRaw =
        mockMvc
            .perform(
                post("/weightlifting/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(exJson))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("\"", "");
    UUID exId = UUID.fromString(exIdRaw);

    // Act: DELETE /weightlifting/exercises/{id}
    mockMvc.perform(delete("/weightlifting/exercises/" + exId)).andExpect(status().isNoContent());

    // Assert: removed
    assertThat(exerciseRepo.findById(exId)).isEmpty();
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
