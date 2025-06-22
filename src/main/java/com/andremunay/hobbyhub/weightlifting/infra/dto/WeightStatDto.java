package com.andremunay.hobbyhub.weightlifting.infra.dto;

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
  private UUID workoutId;
  private LocalDate date;
  private double oneRepMax;
}
