package com.andremunay.hobbyhub.spanish.infra.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object used when creating a new flashcard.
 *
 * <p>Includes only the front and back fields, validated to ensure content is provided.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardDto {
  @NotBlank private String front;

  @NotBlank private String back;
}
