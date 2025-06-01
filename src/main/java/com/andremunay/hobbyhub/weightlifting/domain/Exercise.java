package com.andremunay.hobbyhub.weightlifting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Exercise {

  @Id
  @Column(columnDefinition = "UUID")
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(name = "muscle_group", nullable = false)
  private String muscleGroup;
}
