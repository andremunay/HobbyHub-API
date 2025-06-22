package com.andremunay.hobbyhub.weightlifting.infra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for creating or retrieving a workout.
 *
 * <p>Includes the workout date and one or more associated sets. The ID must be null when creating a
 * new workout to prevent client-side injection.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkoutDto {
  @Null UUID workoutId;

  @NotNull private LocalDate performedOn;

  @Valid
  @Size(min = 1, message = "At least one set is required")
  private List<WorkoutSetDto> sets;
}
