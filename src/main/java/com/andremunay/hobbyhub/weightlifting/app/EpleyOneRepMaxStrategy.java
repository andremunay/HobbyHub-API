package com.andremunay.hobbyhub.weightlifting.app;

import org.springframework.stereotype.Service;

@Service
public class EpleyOneRepMaxStrategy implements OneRepMaxStrategy {

  @Override
  public double calculate(double weightKg, int reps) {
    if (reps <= 0) throw new IllegalArgumentException("Reps must be >= 1");
    return weightKg * (1 + (double) reps / 30);
  }
}
