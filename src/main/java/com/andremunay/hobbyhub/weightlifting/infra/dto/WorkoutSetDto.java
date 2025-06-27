package com.andremunay.hobbyhub.weightlifting.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for submitting or retrieving an individual set in a workout.
 *
 * <p>Each set includes the exercise performed, weight, reps, and order within the workout. -
 * `workoutId` must be null on creation; it's inferred from context. - `order` must be unique within
 * a workout to distinguish sets.
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkoutSetDto {
  @JsonProperty(access = Access.READ_ONLY)
  @Null
  private UUID workoutId;

  @JsonIgnore
  @Schema(hidden = true)
  @Null
  private UUID exerciseId;

  @NotNull private String exerciseName;

  @NotNull
  @DecimalMin("0.1")
  private BigDecimal weightKg;

  @NotNull
  @Min(1)
  private Integer reps;

  @NotNull
  @Min(0)
  private Integer order;
}
