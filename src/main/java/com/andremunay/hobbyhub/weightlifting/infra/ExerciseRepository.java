package com.andremunay.hobbyhub.weightlifting.infra;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link Exercise} entities.
 *
 * <p>Inherits standard CRUD operations from {@link JpaRepository}.
 */
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

  /** Look up an Exercise by its normalized name (all lowercase, no spaces/punctuation). */
  Optional<Exercise> findByName(String name);

  boolean existsByName(String name);
}
