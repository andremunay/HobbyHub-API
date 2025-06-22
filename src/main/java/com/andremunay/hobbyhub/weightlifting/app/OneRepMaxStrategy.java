package com.andremunay.hobbyhub.weightlifting.app;

/**
 * Strategy interface for calculating one-rep max (1RM) based on weight and repetitions.
 *
 * <p>Allows interchangeable use of different estimation formulas (e.g., Epley, Brzycki).
 */
public interface OneRepMaxStrategy {
  /**
   * Estimates the one-repetition maximum (1RM) given the weight and number of repetitions.
   *
   * @param weightKg the weight lifted, in kilograms
   * @param reps the number of consecutive reps completed at that weight
   * @return the estimated 1RM in kilograms
   * @throws IllegalArgumentException if reps is less than 1
   */
  double calculate(double weightKg, int reps);
}
