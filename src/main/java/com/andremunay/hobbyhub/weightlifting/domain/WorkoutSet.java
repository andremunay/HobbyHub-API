package com.andremunay.hobbyhub.weightlifting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  public WorkoutSet(WorkoutSetId id, Exercise exercise, BigDecimal weightKg, int reps) {
    this.id = id;
    this.exercise = exercise;
    this.weightKg = weightKg;
    this.reps = reps;
  }
}
