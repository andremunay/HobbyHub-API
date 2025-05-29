package com.andremunay.hobbyhub.spanish.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flashcards")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Flashcard {

  @Id private UUID id;

  @Column(nullable = false)
  private String front;

  @Column(nullable = false)
  private String back;

  private int repetition;
  private double easinessFactor;

  @Column(name = "interval_days")
  private int interval;

  private LocalDate nextReviewOn;

  public Flashcard(UUID id, String front, String back) {
    this.id = id;
    this.front = front;
    this.back = back;
    this.repetition = 0;
    this.easinessFactor = 2.5;
    this.interval = 1;
    this.nextReviewOn = LocalDate.now();
  }
}
