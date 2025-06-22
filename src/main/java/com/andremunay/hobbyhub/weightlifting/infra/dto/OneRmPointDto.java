package com.andremunay.hobbyhub.weightlifting.infra.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object representing a single one-rep max (1RM) data point.
 *
 * <p>Used for plotting strength progress over time per workout.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OneRmPointDto {
  private UUID workoutId;
  private LocalDate date;
  private double oneRepMax;
}
