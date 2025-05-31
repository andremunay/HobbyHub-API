package com.andremunay.hobbyhub.weightlifting.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class EpleyOneRepMaxStrategyTest {

  private final OneRepMaxStrategy strategy = new EpleyOneRepMaxStrategy();

  @ParameterizedTest(name = "weight={0}, reps={1} -> expected 1RM={2}")
  @CsvSource({"100,1,100.0000", "100,5,116.6667", "80,10,106.6667", "60,20,100.000"})
  void calculateEpleyOneRepMax(double weight, int reps, double expected) {
    double actual = strategy.calculate(weight, reps);
    assertThat(actual)
        .withFailMessage("Expected %.4f but was %.4f", expected, actual)
        .isCloseTo(expected, within(1e-3));
  }

  @Test
  void throwsOnInvalidReps() {
    assertThatThrownBy(() -> strategy.calculate(100, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Reps must be");
  }
}
