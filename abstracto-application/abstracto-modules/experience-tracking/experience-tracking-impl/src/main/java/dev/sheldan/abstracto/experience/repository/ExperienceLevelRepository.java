package dev.sheldan.abstracto.experience.repository;

import dev.sheldan.abstracto.experience.models.database.AExperienceLevel;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository to manage the access to the table managed by {@link AExperienceLevel}
 */
@Repository
public interface ExperienceLevelRepository extends JpaRepository<AExperienceLevel, Integer> {
    @NotNull
    @Override
    Optional<AExperienceLevel> findById(@NonNull Integer aLong);

    @Override
    boolean existsById(@NonNull Integer aLong);

    @NotNull
    @Override
    List<AExperienceLevel> findAll();
}
