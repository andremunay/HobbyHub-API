package com.andremunay.hobbyhub.weightlifting.app;

import org.springframework.stereotype.Service;

/**
 * Implementation of the Epley formula for estimating one-rep max (1RM) in weightlifting.
 *
 * <p>Formula: - If reps = 1, 1RM = weight - Else, 1RM = weight × (1 + reps / 30)
 *
 * <p>Reference: https://en.wikipedia.org/wiki/One-repetition_maximum#Epley_formula
 */
@Service
public class EpleyOneRepMaxStrategy implements OneRepMaxStrategy {

  /**
   * Calculates the estimated one-rep max using the Epley formula.
   *
   * @param weightKg the weight lifted in kilograms
   * @param reps the number of repetitions performed
   * @return estimated 1RM in kilograms
   * @throws IllegalArgumentException if reps < 1
   */
  @Override
  public double calculate(double weightKg, int reps) {
    if (reps <= 0) {
      throw new IllegalArgumentException("Reps must be >= 1");
    }
    if (reps == 1) {
      return weightKg;
    }
    return weightKg * (1 + (double) reps / 30);
  }
}
