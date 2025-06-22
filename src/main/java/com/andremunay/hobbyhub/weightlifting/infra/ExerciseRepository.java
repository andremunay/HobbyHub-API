package com.andremunay.hobbyhub.weightlifting.infra;

import com.andremunay.hobbyhub.weightlifting.domain.Exercise;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing {@link Exercise} entities.
 *
 * <p>Inherits standard CRUD operations from {@link JpaRepository}.
 */
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {}
