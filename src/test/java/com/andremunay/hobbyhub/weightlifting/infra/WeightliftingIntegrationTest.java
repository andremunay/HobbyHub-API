package com.andremunay.hobbyhub.weightlifting.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

  @BeforeEach
  void setUp() {
    Exercise exercise = new Exercise(UUID.randomUUID(), "test", "test");
    exerciseRepo.save(exercise);

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

  /** Ensures a workout can be created and an additional set can be added to it. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void createWorkout_thenAddSet_succeeds() throws Exception {
    // 1) Build and POST a new workout using exerciseName instead of ID
    WorkoutDto workoutRequest = new WorkoutDto();
    workoutRequest.setPerformedOn(LocalDate.of(2025, 6, 18));

    WorkoutSetDto initialSet = new WorkoutSetDto();
    initialSet.setOrder(1);
    initialSet.setWeightKg(BigDecimal.valueOf(50));
    initialSet.setReps(5);
    initialSet.setExerciseName("test");

    workoutRequest.setSets(List.of(initialSet));
    String workoutJson = objectMapper.writeValueAsString(workoutRequest);

    MvcResult result =
        mockMvc
            .perform(
                post("/weightlifting/workouts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(workoutJson))
            .andExpect(status().isOk())
            .andReturn();

    UUID workoutId = UUID.fromString(result.getResponse().getContentAsString().replace("\"", ""));

    // 2) Build and POST an additional set using exerciseName
    WorkoutSetDto additionalSet = new WorkoutSetDto();
    additionalSet.setOrder(2);
    additionalSet.setWeightKg(BigDecimal.valueOf(55));
    additionalSet.setReps(4);
    additionalSet.setExerciseName("test"); // use name now

    String setJson = objectMapper.writeValueAsString(additionalSet);

    mockMvc
        .perform(
            post("/weightlifting/workouts/{workoutId}/sets", workoutId)
                .with(csrf())
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
    req.setName("test4");
    req.setMuscleGroup("test4");

    String json = objectMapper.writeValueAsString(req);

    MvcResult mvc =
        mockMvc
            .perform(
                post("/weightlifting/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isOk())
            .andReturn();

    String name = mvc.getResponse().getContentAsString();
    assertThat(exerciseRepo.findByName(name)).isPresent();
  }

  /** Retrieves a workout by ID and verifies its sets are correctly returned in the DTO. */
  @WithMockUser(
      username = "testuser",
      roles = {"USER"})
  @Test
  void getWorkout_withSets_returnsCorrectDto() throws Exception {
    // 1) Build and POST a workout using exerciseName instead of exerciseId
    WorkoutDto workoutReq = new WorkoutDto();
    workoutReq.setPerformedOn(LocalDate.of(2025, 6, 18));

    WorkoutSetDto set = new WorkoutSetDto();
    set.setOrder(1);
    set.setWeightKg(BigDecimal.valueOf(80));
    set.setReps(3);
    set.setExerciseName("test");

    workoutReq.setSets(List.of(set));
    String wJson = objectMapper.writeValueAsString(workoutReq);

    MvcResult postRes =
        mockMvc
            .perform(
                post("/weightlifting/workouts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(wJson))
            .andExpect(status().isOk())
            .andReturn();

    UUID workoutId = UUID.fromString(postRes.getResponse().getContentAsString().replace("\"", ""));

    // 2) GET the created workout by ID
    MvcResult getRes =
        mockMvc
            .perform(get("/weightlifting/workouts/{id}", workoutId))
            .andExpect(status().isOk())
            .andReturn();

    WorkoutDto dto =
        objectMapper.readValue(
            getRes.getResponse().getContentAsString(), new TypeReference<WorkoutDto>() {});

    // 3) Assert the returned DTO matches what we sent
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
    // prepare a WorkoutDto with exerciseName instead of exerciseId
    WorkoutDto wr = new WorkoutDto();
    wr.setPerformedOn(LocalDate.of(2025, 6, 19));
    WorkoutSetDto s = new WorkoutSetDto();
    s.setOrder(1);
    s.setWeightKg(BigDecimal.valueOf(65));
    s.setReps(8);
    s.setExerciseName("test");
    wr.setSets(List.of(s));

    String json = objectMapper.writeValueAsString(wr);

    // create the workout
    String idRaw =
        mockMvc
            .perform(
                post("/weightlifting/workouts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replace("\"", "");
    UUID wid = UUID.fromString(idRaw);

    // delete the workout by ID (this endpoint still uses path-variable ID)
    mockMvc
        .perform(delete("/weightlifting/workouts/{id}", wid).with(csrf()))
        .andExpect(status().isNoContent());

    // verify it’s gone
    assertThat(workoutRepo.findById(wid)).isEmpty();
  }

  /** Verifies that previously created exercises can be retrieved through the API. */
  @Test
  void getAllExercises_returnsCreatedExercises() throws Exception {
    ExerciseDto e1 = new ExerciseDto();
    e1.setName("test2");
    e1.setMuscleGroup("test2");
    ExerciseDto e2 = new ExerciseDto();
    e2.setName("test3");
    e2.setMuscleGroup("test3");

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
    assertTrue(names.containsAll(List.of("test2", "test3")));
  }

  /** Returns 400 Bad Request when an invalid UUID is passed to /workouts/{id}. */
  @Test
  void getWorkout_invalidUuid_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/weightlifting/workouts/not-a-uuid")).andExpect(status().isBadRequest());
  }

  /** Ensures that invalid workout IDs return 404 and missing exerciseName returns 400. */
  @Test
  void deleteInvalidIds_returnAppropriateStatus() throws Exception {
    // invalid workout ID → Not Found
    mockMvc.perform(delete("/weightlifting/workouts/abc")).andExpect(status().isNotFound());

    // missing required exerciseName param → Bad Request
    mockMvc.perform(delete("/weightlifting/exercises")).andExpect(status().isBadRequest());
  }
}
