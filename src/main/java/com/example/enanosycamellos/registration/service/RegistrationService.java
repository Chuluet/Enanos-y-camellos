package com.example.enanosycamellos.registration.service;

import com.example.enanosycamellos.auditlog.service.AuditLogService;
import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.repository.IRaceRepository;
import com.example.enanosycamellos.registration.dto.RaceRegistrationRequest;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.registration.mapper.RegistrationMapper;
import com.example.enanosycamellos.registration.repository.IRaceRegistrationRepository;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.example.enanosycamellos.team.repository.ITeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Registration business rules. This is the module that ties Race, Competitor
 * and Team together, so it depends on all three repositories — it only
 * reads from them, it never writes to Race/Competitor/Team directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private static final Set<RegistrationStatus> ACTIVE_STATUSES =
            Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);

    private final IRaceRegistrationRepository registrationRepository;
    private final IRaceRepository raceRepository;
    private final ICompetitorRepository competitorRepository;
    private final ITeamRepository teamRepository;
    private final AuditLogService auditLogService;

    // ============================== Read ===============================

    @Transactional(readOnly = true)
    public List<RaceRegistrationResponse> getByRace(UUID raceId) {
        findRaceOrThrow(raceId);
        return RegistrationMapper.toResponseList(registrationRepository.findAllByRace_Id(raceId));
    }

    @Transactional(readOnly = true)
    public RaceRegistrationResponse getById(UUID id) {
        return RegistrationMapper.toResponse(findRegistrationOrThrow(id));
    }

    // =========================== Utilities ==============================

    private Race findRaceOrThrow(UUID id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Race", id));
    }

    private RaceRegistration findRegistrationOrThrow(UUID id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Registration", id));
    }

    // ============================= Write ==============================

    @Transactional
    public RaceRegistrationResponse register(UUID raceId, RaceRegistrationRequest request) {
        if (!request.hasExactlyOneParticipant()) {
            throw new BadRequestException(
                    "Send exactly one of competitorId or teamId, never both or neither");
        }

        Race race = findRaceOrThrow(raceId);
        validateRaceIsOpenForRegistration(race);
        validateRegistrationTypeMatchesRaceType(race, request);

        RaceRegistration registration = request.isIndividual()
                ? buildIndividualRegistration(race, request)
                : buildTeamRegistration(race, request);

        validateStartingPositionIsAvailable(raceId, request.startingPosition());

        RaceRegistration saved = registrationRepository.save(registration);

        auditLogService.log("CREATE", "Registration", saved.getId().toString(),
                "%s registration created for race %s".formatted(
                        request.isIndividual() ? "Individual" : "Team", raceId));

        log.info("Registration created id={} race={} type={}",
                saved.getId(), raceId, request.isIndividual() ? "INDIVIDUAL" : "TEAM");
        return RegistrationMapper.toResponse(saved);
    }

    private void validateRaceIsOpenForRegistration(Race race) {
        if (race.getStatus() != RaceStatus.OPEN_FOR_REGISTRATION) {
            throw new ConflictException(
                    "Race is not open for registration (current status: %s)".formatted(race.getStatus()));
        }
        if (!LocalDateTime.now().isBefore(race.getRegistrationDeadline())) {
            throw new ConflictException("Registration deadline for this race has passed");
        }
    }

    private void validateRegistrationTypeMatchesRaceType(Race race, RaceRegistrationRequest request) {
        boolean individual = request.isIndividual();
        if (race.getRaceType() == RaceType.INDIVIDUAL && !individual) {
            throw new BadRequestException("This race only accepts individual competitors");
        }
        if (race.getRaceType() == RaceType.TEAM && individual) {
            throw new BadRequestException("This race only accepts teams");
        }
        // MIXED accepts either, nothing to check here.
    }

    private RaceRegistration buildIndividualRegistration(Race race, RaceRegistrationRequest request) {
        Competitor competitor = competitorRepository.findById(request.competitorId())
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", request.competitorId()));

        if (competitor.getStatus() != CompetitorStatus.ACTIVE) {
            throw new ConflictException(
                    "Competitor is not ACTIVE (current status: %s)".formatted(competitor.getStatus()));
        }

        if (registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(
                race.getId(), competitor.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Competitor is already registered in this race");
        }

        boolean alreadyInARegisteredTeam = registrationRepository.findAllByRace_Id(race.getId()).stream()
                .filter(r -> ACTIVE_STATUSES.contains(r.getStatus()) && r.getTeam() != null)
                .anyMatch(r -> r.getTeam().hasMember(competitor));
        if (alreadyInARegisteredTeam) {
            throw new ConflictException(
                    "Competitor is already registered in this race as part of a team");
        }

        return RegistrationMapper.toEntity(
                race, competitor, null, request.startingPosition(), request.registeredBy());
    }

    private RaceRegistration buildTeamRegistration(Race race, RaceRegistrationRequest request) {
        Team team = teamRepository.findWithMembersById(request.teamId())
                .orElseThrow(() -> ResourceNotFoundException.of("Team", request.teamId()));

        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new ConflictException(
                    "Team is not ACTIVE (current status: %s)".formatted(team.getStatus()));
        }

        if (team.getMembers().isEmpty()) {
            throw new ConflictException("Team must have at least one member to enter a race");
        }

        boolean allMembersEligible = team.getMembers().stream()
                .allMatch(member -> member.getStatus() == CompetitorStatus.ACTIVE);
        if (!allMembersEligible) {
            throw new ConflictException("All team members must be ACTIVE to enter a race");
        }

        if (registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(
                race.getId(), team.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Team is already registered in this race");
        }

        List<UUID> memberIds = team.getMembers().stream().map(Competitor::getId).toList();
        if (registrationRepository.existsByRace_IdAndCompetitor_IdInAndStatusIn(
                race.getId(), memberIds, ACTIVE_STATUSES)) {
            throw new ConflictException(
                    "One or more team members are already registered individually in this race");
        }

        return RegistrationMapper.toEntity(
                race, null, team, request.startingPosition(), request.registeredBy());
    }

    private void validateStartingPositionIsAvailable(UUID raceId, Integer startingPosition) {
        if (startingPosition == null) return;

        if (registrationRepository.existsByRace_IdAndStartingPositionAndStatusIn(
                raceId, startingPosition, ACTIVE_STATUSES)) {
            throw new ConflictException(
                    "Starting position %d is already taken in this race".formatted(startingPosition));
        }
    }

    @Transactional
    public RaceRegistrationResponse approve(UUID id) {
        RaceRegistration registration = findRegistrationOrThrow(id);

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException(
                    "Only PENDING registrations can be approved (current status: %s)"
                            .formatted(registration.getStatus()));
        }

        registration.setStatus(RegistrationStatus.APPROVED);
        RaceRegistration saved = registrationRepository.save(registration);

        auditLogService.log("APPROVE", "Registration", id.toString(), "Registration approved");

        log.info("Registration id={} approved", id);
        return RegistrationMapper.toResponse(saved);
    }

    @Transactional
    public RaceRegistrationResponse reject(UUID id, String reason) {
        RaceRegistration registration = findRegistrationOrThrow(id);

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException(
                    "Only PENDING registrations can be rejected (current status: %s)"
                            .formatted(registration.getStatus()));
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setValidationNotes(reason);
        RaceRegistration saved = registrationRepository.save(registration);

        auditLogService.log("REJECT", "Registration", id.toString(),
                "Registration rejected: %s".formatted(reason));

        log.info("Registration id={} rejected: {}", id, reason);
        return RegistrationMapper.toResponse(saved);
    }

    @Transactional
    public void cancel(UUID id) {
        RaceRegistration registration = findRegistrationOrThrow(id);

        if (registration.getStatus() == RegistrationStatus.REJECTED
                || registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new ConflictException(
                    "Registration is already %s".formatted(registration.getStatus()));
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        auditLogService.log("CANCEL", "Registration", id.toString(), "Registration cancelled");

        log.info("Registration id={} cancelled", id);
    }
}