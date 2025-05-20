package com.andremunay.hobbyhub.spanish.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flashcards")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Flashcard {

  @Id private UUID id;

  @Column(nullable = false)
  private String front;

  @Column(nullable = false)
  private String back;

  private int repetition;
  private double easinessFactor;
  private LocalDate nextReviewOn;

  public Flashcard(UUID id, String front, String back) {
    this.id = id;
    this.front = front;
    this.back = back;
    this.repetition = 0;
    this.easinessFactor = 2.5;
    this.nextReviewOn = LocalDate.now();
  }
}
