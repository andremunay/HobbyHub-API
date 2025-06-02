package com.andremunay.hobbyhub.weightlifting.infra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Request body for creating a new Workout with nested sets. */
@Getter
@Setter
public class WorkoutCreateRequest {
  @NotNull private LocalDate performedOn;

  @Valid
  @Size(min = 1, message = "At least one set is required")
  private List<WorkoutSetCreateRequest> sets;
}
