package com.example.enanosycamellos.race.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.dto.RaceUpdateRequest;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.mapper.RaceMapper;
import com.example.enanosycamellos.auditlog.service.AuditLogService;
import com.example.enanosycamellos.race.repository.IRaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaceService {

    private final IRaceRepository raceRepository;
     private final AuditLogService auditLogService;

    private static final Map<RaceStatus, Set<RaceStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RaceStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(RaceStatus.DRAFT,
                EnumSet.of(RaceStatus.OPEN_FOR_REGISTRATION, RaceStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RaceStatus.OPEN_FOR_REGISTRATION,
                EnumSet.of(RaceStatus.CLOSED_FOR_REGISTRATION, RaceStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RaceStatus.CLOSED_FOR_REGISTRATION,
                EnumSet.of(RaceStatus.IN_PROGRESS, RaceStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RaceStatus.IN_PROGRESS,
                EnumSet.of(RaceStatus.COMPLETED, RaceStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RaceStatus.COMPLETED, EnumSet.noneOf(RaceStatus.class));
        ALLOWED_TRANSITIONS.put(RaceStatus.CANCELLED, EnumSet.noneOf(RaceStatus.class));
    }

    @Transactional(readOnly = true)
    public List<RaceResponse> getRaces(RaceStatus status, RaceType type) {
        List<Race> races;
        if (status != null && type != null) {
            races = raceRepository.findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc(status, type);
        } else if (status != null) {
            races = raceRepository.findAllByStatusOrderByScheduledDateTimeAsc(status);
        } else if (type != null) {
            races = raceRepository.findAllByRaceTypeOrderByScheduledDateTimeAsc(type);
        } else {
            races = raceRepository.findAllByOrderByScheduledDateTimeAsc();
        }
        return RaceMapper.toResponseList(races);
    }

    @Transactional(readOnly = true)
    public RaceResponse getById(UUID id) {
        Race race = findOrThrow(id);
        return RaceMapper.toResponse(race);
    }

    @Transactional
    public RaceResponse create(RaceRequest request) {
        if (!request.isDeadlineBeforeStart()) {
            throw new BadRequestException(
                    "registrationDeadline must be earlier than scheduledDateTime");
        }

        Race race = RaceMapper.toEntity(request);
        Race saved = raceRepository.save(race);

        auditLogService.log("CREATE", "Race", saved.getId().toString(),
                "Race '%s' created".formatted(saved.getName()));
        log.info("Race created id={} name={} type={}", saved.getId(), saved.getName(), saved.getRaceType());
        return RaceMapper.toResponse(saved);
    }

    @Transactional
    public RaceResponse update(UUID id, RaceUpdateRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("The request has no fields to update");
        }

        Race race = findOrThrow(id);

        if (race.getStatus() == RaceStatus.COMPLETED) {
            throw new ConflictException("A completed race cannot be edited");
        }

        LocalDateTime effectiveScheduledDateTime =
                request.scheduledDateTime() != null ? request.scheduledDateTime() : race.getScheduledDateTime();
        LocalDateTime effectiveDeadline =
                request.registrationDeadline() != null ? request.registrationDeadline() : race.getRegistrationDeadline();

        if (!effectiveDeadline.isBefore(effectiveScheduledDateTime)) {
            throw new BadRequestException(
                    "registrationDeadline must be earlier than scheduledDateTime");
        }

        if (request.name() != null) race.setName(request.name());
        if (request.description() != null) race.setDescription(request.description());
        if (request.scheduledDateTime() != null) race.setScheduledDateTime(request.scheduledDateTime());
        if (request.startLocation() != null) race.setStartLocation(request.startLocation());
        if (request.finishLocation() != null) race.setFinishLocation(request.finishLocation());
        if (request.distanceMeters() != null) race.setDistanceMeters(request.distanceMeters());
        if (request.maxParticipants() != null) race.setMaxParticipants(request.maxParticipants());
        if (request.registrationDeadline() != null) race.setRegistrationDeadline(request.registrationDeadline());

        Race updated = raceRepository.save(race);
        auditLogService.log("UPDATE", "Race", id.toString(), "Race data updated");
        log.info("Race updated id={}", id);
        return RaceMapper.toResponse(updated);
    }

    @Transactional
    public RaceResponse changeStatus(UUID id, RaceStatus newStatus) {
        Race race = findOrThrow(id);
        RaceStatus current = race.getStatus();

        if (current == newStatus) {
            throw new ConflictException(
                    "Race is already in status %s".formatted(newStatus));
        }

        Set<RaceStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowedTargets.contains(newStatus)) {
            throw new ConflictException(
                    "Cannot move race from %s to %s".formatted(current, newStatus));
        }

        if (newStatus == RaceStatus.IN_PROGRESS) {
            // TODO(registrations): reject if approved registrations < 2.
            log.debug("Starting race id={} (participant count not yet validated)", id);
        }

        if (newStatus == RaceStatus.COMPLETED) {
            // TODO(results): reject if this race has no RaceResult rows yet.
            log.debug("Completing race id={} (results not yet validated)", id);
        }

        race.setStatus(newStatus);
        Race saved = raceRepository.save(race);

        String action = newStatus == RaceStatus.CANCELLED ? "CANCEL" : "STATUS_CHANGE";
        auditLogService.log(action, "Race", id.toString(),
                "Race status changed from %s to %s".formatted(current, newStatus),
                current, newStatus);
                
        log.info("Race id={} moved from {} to {}", id, current, newStatus);
        return RaceMapper.toResponse(saved);
    }

    @Transactional
    public void cancel(UUID id) {
        changeStatus(id, RaceStatus.CANCELLED);
    }

    private Race findOrThrow(UUID id) {
        return raceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Race", id));
    }
}