package com.andremunay.hobbyhub.weightlifting.infra.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.validation.constraints.Null;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object representing a single data point for weightlifting progress.
 *
 * <p>Used to visualize one-rep max trends over time for a specific workout.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeightStatDto {
  @JsonProperty(access = Access.READ_ONLY)
  @Null
  private UUID workoutId;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate date;

  private double oneRepMax;
}
