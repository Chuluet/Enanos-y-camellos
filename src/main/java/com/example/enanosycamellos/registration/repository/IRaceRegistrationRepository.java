package com.example.enanosycamellos.registration.repository;

import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IRaceRegistrationRepository extends JpaRepository<RaceRegistration, UUID> {

    List<RaceRegistration> findAllByRace_Id(UUID raceId);

    /** Used to enforce "a competitor/team cannot be registered twice in the same race". */
    boolean existsByRace_IdAndCompetitor_IdAndStatusIn(
            UUID raceId, UUID competitorId, Collection<RegistrationStatus> statuses);

    boolean existsByRace_IdAndTeam_IdAndStatusIn(
            UUID raceId, UUID teamId, Collection<RegistrationStatus> statuses);

    /**
     * Used to enforce "a participant cannot compete simultaneously as an
     * individual and as a team member in the same race": checks whether any
     * of a team's members already has an individual registration.
     */
    boolean existsByRace_IdAndCompetitor_IdInAndStatusIn(
            UUID raceId, Collection<UUID> competitorIds, Collection<RegistrationStatus> statuses);

    /** Used to enforce "starting positions cannot be duplicated" within a race. */
    boolean existsByRace_IdAndStartingPositionAndStatusIn(
            UUID raceId, Integer startingPosition, Collection<RegistrationStatus> statuses);

    /**
     * Used by RaceService's pending TODO: count of APPROVED registrations,
     * to validate "at least two valid participants are required to start".
     */
    long countByRace_IdAndStatus(UUID raceId, RegistrationStatus status);
}