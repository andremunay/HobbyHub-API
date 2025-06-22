package com.andremunay.hobbyhub.weightlifting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite primary key for {@link WorkoutSet}, consisting of: - the parent workout's ID - the
 * set's order within that workout
 *
 * <p>This enables uniquely identifying sets within a workout session.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class WorkoutSetId implements Serializable {
  @Column(name = "workout_id", columnDefinition = "UUID")
  private UUID workoutId;

  @Column(name = "set_order")
  private int order;
}
