package com.example.enanosycamellos.result.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.repository.IRaceRepository;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.registration.repository.IRaceRegistrationRepository;
import com.example.enanosycamellos.result.dto.RaceResultRequest;
import com.example.enanosycamellos.result.dto.RaceResultResponse;
import com.example.enanosycamellos.result.dto.RaceResultUpdateRequest;
import com.example.enanosycamellos.result.entity.RaceResult;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.result.mapper.RaceResultMapper;
import com.example.enanosycamellos.result.repository.IRaceResultRepository;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.repository.ITeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Result business rules. Also responsible for keeping the stats counters on
 * Competitor/Team (victories, defeats, completedRaces) consistent whenever a
 * result is recorded or edited — see {@link #applyStatistics}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaceResultService {

    private final IRaceResultRepository resultRepository;
    private final IRaceRegistrationRepository registrationRepository;
    private final IRaceRepository raceRepository;
    private final ICompetitorRepository competitorRepository;
    private final ITeamRepository teamRepository;

    // ============================== Read ===============================

    @Transactional(readOnly = true)
    public List<RaceResultResponse> getByRace(UUID raceId) {
        findRaceOrThrow(raceId);
        return RaceResultMapper.toResponseList(resultRepository.findAllByRegistration_Race_Id(raceId));
    }

    @Transactional(readOnly = true)
    public RaceResultResponse getById(UUID id) {
        return RaceResultMapper.toResponse(findResultOrThrow(id));
    }

    // ============================= Write ==============================

    @Transactional
    public RaceResultResponse record(RaceResultRequest request) {
        if (!request.isCompletionTimeConsistentWithStatus()) {
            throw new BadRequestException("completionTime is mandatory and must be positive when status is FINISHED");
        }
        if (!request.isFinalPositionConsistentWithStatus()) {
            throw new BadRequestException("finalPosition can only be set when status is FINISHED");
        }

        RaceRegistration registration = registrationRepository.findById(request.registrationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Registration", request.registrationId()));

        validateRegistrationIsApproved(registration);
        validateRaceIsInProgress(registration.getRace());

        if (resultRepository.existsByRegistration_Id(registration.getId())) {
            throw new ConflictException("This registration already has a result recorded");
        }

        Integer startPosition = request.startPosition() != null
                ? request.startPosition()
                : registration.getStartingPosition();

        validatePositionIsAvailable(registration.getRace().getId(), request.finalPosition(), UUID.randomUUID());

        RaceResult result = RaceResultMapper.toEntity(
                registration, startPosition, request.finalPosition(), request.completionTime(),
                request.penaltyTime(), request.status(), request.notes(), request.recordedBy());

        RaceResult saved = resultRepository.save(result);
        applyStatistics(registration, request.status(), request.finalPosition(), 1);

        log.info("Result recorded id={} registration={} status={}",
                saved.getId(), registration.getId(), request.status());
        return RaceResultMapper.toResponse(saved);
    }
    
    @Transactional
    public RaceResultResponse update(UUID id, RaceResultUpdateRequest request) {
        if (!request.isCompletionTimeConsistentWithStatus()) {
            throw new BadRequestException("completionTime is mandatory and must be positive when status is FINISHED");
        }
        if (!request.isFinalPositionConsistentWithStatus()) {
            throw new BadRequestException("finalPosition can only be set when status is FINISHED");
        }

        RaceResult result = findResultOrThrow(id);
        RaceRegistration registration = result.getRegistration();

        validateRaceIsInProgress(registration.getRace());
        validatePositionIsAvailable(registration.getRace().getId(), request.finalPosition(), id);

        // Undo the stats this result had applied before, so we never double-count.
        applyStatistics(registration, result.getStatus(), result.getFinalPosition(), -1);

        Integer startPosition = request.startPosition() != null ? request.startPosition() : result.getStartPosition();
        result.setStartPosition(startPosition);
        result.setFinalPosition(request.finalPosition());
        result.setCompletionTime(request.completionTime());
        result.setPenaltyTime(request.penaltyTime() != null ? request.penaltyTime() : 0.0);
        result.setStatus(request.status());
        result.setNotes(request.notes());
        result.setRecordedBy(request.recordedBy());

        RaceResult saved = resultRepository.save(result);
        applyStatistics(registration, request.status(), request.finalPosition(), 1);

        log.info("Result id={} updated, new status={}", id, request.status());
        return RaceResultMapper.toResponse(saved);
    }

    /**
     * Keeps Competitor/Team stats consistent with the results recorded for
     * them. Call with {@code sign = +1} to apply a result's effect, and
     * {@code sign = -1} to undo it (used by {@link #update} before applying
     * the new values, so editing a result never double-counts).
     *
     * <p>Only FINISHED results count towards stats: a disqualified/DNF/DNS
     * participant didn't complete the race, so it shouldn't move their
     * completedRaces/victories/defeats either way.</p>
     */
    private void applyStatistics(RaceRegistration registration, ResultStatus status,
                                  Integer finalPosition, int sign) {
        if (status != ResultStatus.FINISHED) return;

        boolean isWinner = finalPosition != null && finalPosition == 1;

        if (registration.getCompetitor() != null) {
            Competitor competitor = registration.getCompetitor();
            competitor.setCompletedRaces(competitor.getCompletedRaces() + sign);
            if (isWinner) {
                competitor.setVictories(competitor.getVictories() + sign);
            } else {
                competitor.setDefeats(competitor.getDefeats() + sign);
            }
            competitorRepository.save(competitor);
            return;
        }

        Team team = registration.getTeam();
        if (isWinner) {
            team.setVictories(team.getVictories() + sign);
        } else {
            team.setDefeats(team.getDefeats() + sign);
        }
        teamRepository.save(team);

        for (Competitor member : team.getMembers()) {
            member.setCompletedRaces(member.getCompletedRaces() + sign);
            if (isWinner) {
                member.setVictories(member.getVictories() + sign);
            } else {
                member.setDefeats(member.getDefeats() + sign);
            }
            competitorRepository.save(member);
        }
    }

    private void validateRegistrationIsApproved(RaceRegistration registration) {
        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new ConflictException(
                    "Only APPROVED registrations can receive a result (current status: %s)"
                            .formatted(registration.getStatus()));
        }
    }

    private void validateRaceIsInProgress(Race race) {
        if (race.getStatus() != RaceStatus.IN_PROGRESS) {
            throw new ConflictException(
                    "Results can only be recorded while the race is IN_PROGRESS (current status: %s)"
                            .formatted(race.getStatus()));
        }
    }

    private void validatePositionIsAvailable(UUID raceId, Integer finalPosition, UUID excludedResultId) {
        if (finalPosition == null) return;
        if (resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                raceId, finalPosition, ResultStatus.FINISHED, excludedResultId)) {
            throw new ConflictException(
                    "Final position %d is already taken in this race".formatted(finalPosition));
        }
    }

    private Race findRaceOrThrow(UUID id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Race", id));
    }

    private RaceResult findResultOrThrow(UUID id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Result", id));
    }
}