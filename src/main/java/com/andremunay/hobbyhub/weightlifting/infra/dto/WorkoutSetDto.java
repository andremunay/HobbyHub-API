package com.andremunay.hobbyhub.weightlifting.infra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents one set in a WorkoutDto. */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkoutSetDto {
  @Null private UUID workoutId;

  @NotNull private UUID exerciseId;

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
