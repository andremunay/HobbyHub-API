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

  public Workout(UUID id, LocalDate performedOn) {
    this.id = id;
    this.performedOn = performedOn;
  }

  public void addSet(WorkoutSet set) {
    sets.add(set);
    set.setWorkout(this);
  }
}
