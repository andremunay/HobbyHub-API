package com.andremunay.hobbyhub.weightlifting.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.andremunay.hobbyhub.weightlifting.infra.dto.WeightStatDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutDto;
import com.andremunay.hobbyhub.weightlifting.infra.dto.WorkoutSetDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

/**
 * Full integration tests for the weightlifting module.
 *
 * <p>These tests use a real Spring Boot context, MockMvc for HTTP interaction, and
 * Testcontainers-backed PostgreSQL for data persistence.
 */
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
    Exercise exercise = new Exercise(UUID.randomUUID(), "Bench Press", "Chest");
    exerciseRepo.save(exercise);
    this.exerciseId = exercise.getId();

    Workout w1 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 1));
    Workout w2 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 2));
    Workout w3 = new Workout(UUID.randomUUID(), LocalDate.of(2025, 5, 3));

    workoutRepo.saveAll(List.of(w1, w2, w3));

    WorkoutSet s1 =
        new WorkoutSet(new WorkoutSetId(w1.getId(), 1), exercise, BigDecimal.valueOf(50), 5);
    s1.setWorkout(w1);

    WorkoutSet s2 =
        new WorkoutSet(new WorkoutSetId(w2.getId(), 1), exercise, BigDecimal.valueOf(60), 5);
    s2.setWorkout(w2);

    WorkoutSet s3 =
        new WorkoutSet(new WorkoutSetId(w3.getId(), 1), exercise, BigDecimal.valueOf(70), 5);
    s3.setWorkout(w3);

    w1.addSet(s1);
    w2.addSet(s2);
    w3.addSet(s3);

    workoutRepo.saveAll(List.of(w1, w2, w3));
  }

  /**
   * Verifies that the 1RM stats endpoint returns ascending dates and values computed with the Epley
   * formula.
   */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void getOneRepMaxStats_returnsAscendingDatesAndCorrectValues() throws Exception {
    String url = "/weightlifting/stats/1rm/" + exerciseId + "?lastN=3";

    MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();

    String json = result.getResponse().getContentAsString();
    List<WeightStatDto> stats = objectMapper.readValue(json, new TypeReference<>() {});

    assertThat(stats).isNotNull().hasSize(3);

    assertThat(stats.get(0).getDate()).isEqualTo(LocalDate.of(2025, 5, 1));
    assertThat(stats.get(1).getDate()).isEqualTo(LocalDate.of(2025, 5, 2));
    assertThat(stats.get(2).getDate()).isEqualTo(LocalDate.of(2025, 5, 3));

    double rm1 = stats.get(0).getOneRepMax();
    double rm2 = stats.get(1).getOneRepMax();
    double rm3 = stats.get(2).getOneRepMax();

    assertThat(rm1).isCloseTo(58.3333, within(1e-3));
    assertThat(rm2).isCloseTo(70.0000, within(1e-3));
    assertThat(rm3).isCloseTo(81.6667, within(1e-3));
  }

  /** Ensures a workout can be created and an additional set can be added to it. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void createWorkout_thenAddSet_succeeds() throws Exception {
    WorkoutDto workoutRequest = new WorkoutDto();
    workoutRequest.setPerformedOn(LocalDate.of(2025, 6, 18));

    WorkoutSetDto initialSet = new WorkoutSetDto();
    initialSet.setOrder(1);
    initialSet.setWeightKg(BigDecimal.valueOf(50));
    initialSet.setReps(5);
    initialSet.setExerciseId(exerciseId);

    workoutRequest.setSets(List.of(initialSet));

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

  /** Tests that a new exercise can be successfully created and persisted. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void createExercise_succeeds() throws Exception {
    ExerciseDto req = new ExerciseDto();
    req.setName("Squat");
    req.setMuscleGroup("Legs");

    String json = objectMapper.writeValueAsString(req);

    MvcResult mvc =
        mockMvc
            .perform(
                post("/weightlifting/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isOk())
            .andReturn();

    UUID id = UUID.fromString(mvc.getResponse().getContentAsString().replace("\"", ""));
    assertThat(exerciseRepo.findById(id)).isPresent();
  }

  /** Retrieves a workout by ID and verifies its sets are correctly returned in the DTO. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void getWorkout_withSets_returnsCorrectDto() throws Exception {
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

    MvcResult getRes =
        mockMvc
            .perform(get("/weightlifting/workouts/" + workoutId))
            .andExpect(status().isOk())
            .andReturn();

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

  /** Deletes an existing workout and verifies that it no longer exists in the database. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void deleteWorkout_succeeds() throws Exception {
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

    mockMvc.perform(delete("/weightlifting/workouts/" + wid)).andExpect(status().isNoContent());

    assertThat(workoutRepo.findById(wid)).isEmpty();
  }

  /** Deletes an existing exercise and confirms its removal from the database. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void deleteExercise_succeeds() throws Exception {
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

    mockMvc.perform(delete("/weightlifting/exercises/" + exId)).andExpect(status().isNoContent());

    assertThat(exerciseRepo.findById(exId)).isEmpty();
  }

  /** Verifies that previously created exercises can be retrieved through the API. */
  @Test
  void getAllExercises_returnsCreatedExercises() throws Exception {
    ExerciseDto e1 = new ExerciseDto();
    e1.setName("Press");
    e1.setMuscleGroup("Push");
    ExerciseDto e2 = new ExerciseDto();
    e2.setName("Pull");
    e2.setMuscleGroup("Back");

    mockMvc
        .perform(
            post("/weightlifting/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(e1)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/weightlifting/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(e2)))
        .andExpect(status().isOk());

    MvcResult mvc =
        mockMvc
            .perform(get("/weightlifting/exercises").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    List<ExerciseDto> all =
        objectMapper.readValue(
            mvc.getResponse().getContentAsString(), new TypeReference<List<ExerciseDto>>() {});

    List<String> names = all.stream().map(ExerciseDto::getName).collect(Collectors.toList());
    assertTrue(names.containsAll(List.of("Press", "Pull")));
  }

  /** Verifies that previously created workouts are returned by the list endpoint. */
  @Test
  void getAllWorkouts_returnsCreatedWorkout() throws Exception {
    ExerciseDto exDto = new ExerciseDto();
    exDto.setName("Squat");
    exDto.setMuscleGroup("Legs");
    MvcResult exMvc =
        mockMvc
            .perform(
                post("/weightlifting/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(exDto)))
            .andExpect(status().isOk())
            .andReturn();
    UUID exId = UUID.fromString(exMvc.getResponse().getContentAsString().replace("\"", ""));

    WorkoutDto wDto = new WorkoutDto();
    wDto.setPerformedOn(LocalDate.now());
    WorkoutSetDto setDto = new WorkoutSetDto();
    setDto.setExerciseId(exId);
    setDto.setWeightKg(BigDecimal.valueOf(80));
    setDto.setReps(5);
    setDto.setOrder(1);
    wDto.setSets(List.of(setDto));

    mockMvc
        .perform(
            post("/weightlifting/workouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wDto)))
        .andExpect(status().isOk());

    MvcResult mvc =
        mockMvc
            .perform(get("/weightlifting/workouts").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

    List<WorkoutDto> all =
        objectMapper.readValue(
            mvc.getResponse().getContentAsString(), new TypeReference<List<WorkoutDto>>() {});

    assertTrue(
        all.stream().map(WorkoutDto::getPerformedOn).anyMatch(d -> d.equals(LocalDate.now())));
  }

  /** Returns 400 Bad Request when an invalid UUID is passed to /workouts/{id}. */
  @Test
  void getWorkout_invalidUuid_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/weightlifting/workouts/not-a-uuid")).andExpect(status().isBadRequest());
  }

  /** Ensures that invalid UUIDs passed to DELETE routes return 400 Bad Request. */
  @Test
  void deleteInvalidIds_returnBadRequest() throws Exception {
    mockMvc.perform(delete("/weightlifting/workouts/abc")).andExpect(status().isBadRequest());
    mockMvc.perform(delete("/weightlifting/exercises/xyz")).andExpect(status().isBadRequest());
  }
}
