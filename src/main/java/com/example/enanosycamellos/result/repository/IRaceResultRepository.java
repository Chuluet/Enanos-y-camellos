package com.example.enanosycamellos.result.repository;

import com.example.enanosycamellos.result.entity.RaceResult;
import com.example.enanosycamellos.result.entity.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRaceResultRepository extends JpaRepository<RaceResult, UUID> {

    List<RaceResult> findAllByRegistration_Race_Id(UUID raceId);

    Optional<RaceResult> findByRegistration_Id(UUID registrationId);

    boolean existsByRegistration_Id(UUID registrationId);

    /**
     * Used for both "final positions cannot be duplicated" and "only one
     * official winner is allowed" (the latter is just this check with
     * finalPosition = 1): true if some OTHER FINISHED result in the same
     * race already holds that position.
     */
    boolean existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
            UUID raceId, Integer finalPosition, ResultStatus status, UUID excludedResultId);
        
    /** Individual results only, for /api/standings/competitors. */
    List<RaceResult> findAllByRegistration_CompetitorIsNotNull();

    /** Team results only, for /api/standings/teams. */
    List<RaceResult> findAllByRegistration_TeamIsNotNull();
}