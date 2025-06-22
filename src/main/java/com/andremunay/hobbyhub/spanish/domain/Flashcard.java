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

/**
 * JPA entity representing a flashcard used in spaced repetition learning.
 *
 * <p>Tracks both the content and scheduling metadata needed for the SM-2 review algorithm.
 */
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

  @Column(nullable = false)
  private int repetition;

  @Column(nullable = false, name = "easiness_factor")
  private double easinessFactor;

  @Column(nullable = false, name = "interval_days")
  private int interval;

  @Column(nullable = false, name = "next_review_on")
  private LocalDate nextReviewOn;

  /**
   * Constructs a new flashcard with default review settings.
   *
   * <p>Initializes: - repetition = 0 - easinessFactor = 2.5 (standard SM-2 starting value) -
   * interval = 1 day - nextReviewOn = today
   *
   * @param id unique identifier for the flashcard
   * @param front text prompt (e.g. question or word)
   * @param back response (e.g. definition or answer)
   */
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
