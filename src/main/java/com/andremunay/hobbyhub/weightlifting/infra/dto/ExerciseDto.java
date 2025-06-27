package com.andremunay.hobbyhub.weightlifting.infra.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for creating or retrieving exercise information.
 *
 * <p>- `id` must be null on input to prevent client-side ID injection. - `name` and `muscleGroup`
 * are required fields. - Null fields are omitted from serialized JSON.
 */
@Getter
@Setter
@NoArgsConstructor
public class ExerciseDto {
  @JsonIgnore
  @Schema(hidden = true)
  @Null
  private UUID id;

  @NotNull private String name;

  @NotNull private String muscleGroup;
}
