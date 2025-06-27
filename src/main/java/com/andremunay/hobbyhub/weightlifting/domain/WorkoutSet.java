package com.andremunay.hobbyhub.weightlifting.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a single exercise set performed during a workout.
 *
 * <p>Uses a composite key ({@link WorkoutSetId}) to uniquely identify the set by workout and order.
 * Each set is tied to a specific {@link Exercise} and belongs to a parent {@link Workout}.
 */
@SuppressWarnings("java:S7027")
@Entity
@Table(name = "workout_sets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSet {

  @EmbeddedId private WorkoutSetId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("workoutId")
  @JoinColumn(name = "workout_id")
  private Workout workout;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exercise_id", nullable = false)
  private Exercise exercise;

  @Column(name = "weight_kg", nullable = false)
  private BigDecimal weightKg;

  @Column(name = "reps", nullable = false)
  private int reps;

  /**
   * Constructs a set without assigning it to a parent workout (use addSet for linkage).
   *
   * @param id composite ID identifying the workout and order
   * @param exercise the exercise performed
   * @param weightKg weight lifted in kilograms
   * @param reps number of repetitions completed
   */
  public WorkoutSet(WorkoutSetId id, Exercise exercise, BigDecimal weightKg, int reps) {
    this.id = id;
    this.exercise = exercise;
    this.weightKg = weightKg;
    this.reps = reps;
  }
}
