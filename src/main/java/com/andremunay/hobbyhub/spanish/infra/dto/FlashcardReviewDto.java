package com.andremunay.hobbyhub.spanish.infra.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object representing a flashcard scheduled for review.
 *
 * <p>Includes metadata relevant to the review queue, excluding algorithm-specific internals.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardReviewDto {
  @JsonProperty(access = Access.READ_ONLY)
  @Null
  private UUID id;

  @NotBlank private String front;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String back;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate nextReviewOn;
}
