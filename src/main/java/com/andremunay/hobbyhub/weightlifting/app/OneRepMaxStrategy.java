package com.andremunay.hobbyhub.weightlifting.app;

public interface OneRepMaxStrategy {
  /**
   * Estimate the one-rep max given weight and reps.
   *
   * @param weightKg the weight in kg
   * @param reps number of reps performed (>= 1)
   * @return estimated 1RM
   */
  double calculate(double weightKg, int reps);
}
