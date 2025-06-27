package com.andremunay.hobbyhub.spanish.infra.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for submitting a user's review score on a flashcard.
 *
 * <p>Used in the spaced repetition algorithm to adjust scheduling based on recall quality. Grade
 * must be an integer between 0 (complete blackout) and 5 (perfect recall).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardGradeDto {

  @NotBlank private String front;

  @NotNull
  @Min(0)
  @Max(5)
  private Integer grade;
}
