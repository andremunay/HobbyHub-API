package com.andremunay.hobbyhub.spanish.infra.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for grading a flashcard review. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardGradeDto {

  @NotNull
  @Min(0)
  @Max(5)
  private Integer grade;
}
