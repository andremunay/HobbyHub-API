package com.andremunay.hobbyhub.weightlifting.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a workout session containing multiple exercise sets.
 *
 * <p>Workouts are uniquely identified and timestamped, and hold associated sets of lifts.
 */
@Entity
@Table(name = "workouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Workout {

  @Id
  @Column(columnDefinition = "UUID")
  private UUID id;

  @Column(name = "performed_on", nullable = false)
  private LocalDate performedOn;

  @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WorkoutSet> sets = new ArrayList<>();

  /**
   * Convenience constructor to initialize a workout with its date.
   *
   * @param id unique identifier
   * @param performedOn date the workout was completed
   */
  public Workout(UUID id, LocalDate performedOn) {
    this.id = id;
    this.performedOn = performedOn;
  }

  /**
   * Adds a set to the workout and ensures bidirectional linkage.
   *
   * @param set the set to associate with this workout
   */
  public void addSet(WorkoutSet set) {
    sets.add(set);
    set.setWorkout(this);
  }
}
