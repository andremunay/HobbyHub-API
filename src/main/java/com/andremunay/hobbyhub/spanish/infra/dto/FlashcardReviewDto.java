package com.andremunay.hobbyhub.spanish.infra.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardReviewDto {
  private UUID id;
  private String front;
  private String back;
  private LocalDate nextReviewOn;
}
