package com.andremunay.hobbyhub.weightlifting.app;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeightStatDto {
  private UUID workoutId;
  private LocalDate date;
  private double oneRepMax;
}
