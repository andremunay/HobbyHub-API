package com.andremunay.hobbyhub.weightlifting.infra.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Represents one set in a WorkoutCreateRequest. */
@Getter
@Setter
public class WorkoutSetCreateRequest {
  @NotNull private String exerciseId; // UUID of existing Exercise, as string

  @NotNull
  @DecimalMin("0.1")
  private BigDecimal weightKg;

  @NotNull
  @Min(1)
  private Integer reps;

  @NotNull
  @Min(0)
  private Integer order; // the “position” of this set within the workout
}
