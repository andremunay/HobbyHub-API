package com.andremunay.hobbyhub.weightlifting.infra.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO returned by GET /stats/1rm/{exerciseId}, representing a single { workoutId, date, oneRepMax }
 * point.
 */
@Getter
@Setter
@AllArgsConstructor
public class OneRmPoint {
  private UUID workoutId;
  private LocalDate date;
  private double oneRepMax;
}
