package com.example.enanosycamellos.competitor.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.dto.*;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.competitor.mapper.CompetitorMapper;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Competitor business rules.
 *
 * <p>Unlike Cow/Team, there is no separate "assign relationship" flow here:
 * a competitor's team is changed through {@code TeamService.addMember} /
 * {@code removeMember}, not through this service — same reasoning as why
 * {@code CowService} doesn't have a "changeClowns" method, only
 * {@code changeOwner}. Competitor <-> Team is owned from the Team side.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitorService {

    private final ICompetitorRepository competitorRepository;

    // ============================== Read ===============================

    /**
     * Paginated, optionally filtered listing.
     *
     * <p>Module 2 requires filtering, pagination and sorting. Rather than
     * multiplying repository methods for every combination, the four cases
     * (no filter / status only / type only / both) are resolved here, so the
     * controller stays a thin pass-through of {@code Pageable} plus two
     * optional query params.</p>
     */
    @Transactional(readOnly = true)
    public Page<CompetitorResponse> getCompetitors(CompetitorStatus status, CompetitorType type, Pageable pageable) {
        Page<Competitor> page;

        if (status != null && type != null) {
            page = competitorRepository.findAllByStatusAndCompetitorType(status, type, pageable);
        } else if (status != null) {
            page = competitorRepository.findAllByStatus(status, pageable);
        } else if (type != null) {
            page = competitorRepository.findAllByCompetitorType(type, pageable);
        } else {
            page = competitorRepository.findAll(pageable);
        }

        return page.map(CompetitorMapper::toResponse);
    }
    @Transactional(readOnly = true)
    public List<CompetitorResponse> getAll() {
        return competitorRepository.findAll()
                .stream()
                .map(CompetitorMapper::toResponse)
                .toList();
    }

    /** A competitor by id, with its team. Throws 404 if not found. */
    @Transactional(readOnly = true)
    public CompetitorResponse getById(UUID id) {
        Competitor competitor = competitorRepository.findWithTeamById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));
        return CompetitorMapper.toResponse(competitor);
    }

    // ============================= Write ==============================

    /** Creates a competitor. Starts with no team: assignment happens via TeamService. */
    @Transactional
    public CompetitorResponse create(CompetitorRequest request) {
        if (competitorRepository.existsByNicknameIgnoreCase(request.nickname())) {
            throw new ConflictException(
                    "There is already a competitor nicknamed '%s'".formatted(request.nickname()));
        }
        if (request.isMissingAgeInfo()) {
            throw new BadRequestException(
                    "Either dateOfBirth or approximateAge must be provided");
        }

        Competitor competitor = CompetitorMapper.toEntity(request);
        Competitor saved = competitorRepository.save(competitor);

        log.info("Competitor created id={} nickname={}", saved.getId(), saved.getNickname());
        return CompetitorMapper.toResponse(saved);
    }

    /**
     * Full replace via PUT. Team, status and statistics are untouched here:
     * team belongs to TeamService, status has its own endpoint, and the
     * statistics belong to the Results module.
     */
    @Transactional
    public CompetitorResponse update(UUID id, CompetitorUpdateRequest request) {
        Competitor competitor = competitorRepository.findWithTeamById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));

        if (competitorRepository.existsByNicknameIgnoreCaseAndIdNot(request.nickname(), id)) {
            throw new ConflictException(
                    "There is already another competitor nicknamed '%s'".formatted(request.nickname()));
        }
        if (request.isMissingAgeInfo()) {
            throw new BadRequestException(
                    "Either dateOfBirth or approximateAge must be provided");
        }

        competitor.setName(request.name());
        competitor.setNickname(request.nickname());
        competitor.setCompetitorType(request.competitorType());
        competitor.setBirthDate(request.dateOfBirth());
        competitor.setAge(request.approximateAge());
        competitor.setHeight(request.height());
        competitor.setWeight(request.weight());
        competitor.setOrigin(request.origin());

        Competitor updated = competitorRepository.save(competitor);
        log.info("Competitor updated id={}", id);
        return CompetitorMapper.toResponse(updated);
    }

    /**
     * Changes only the status (PATCH /status), e.g. ACTIVE -> INJURED after
     * a race, or -> SUSPENDED by an administrator.
     *
     * <p>RETIRED is terminal: once a competitor retires, no further status
     * change is allowed (not even a no-op "RETIRED -> RETIRED"), whether it
     * happened through this endpoint or through {@link #retire}.</p>
     */
    @Transactional
    public CompetitorResponse changeStatus(UUID id, CompetitorStatusUpdateRequest request) {
        Competitor competitor = competitorRepository.findWithTeamById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));

        if (competitor.getStatus() == CompetitorStatus.RETIRED) {
            throw new ConflictException(
                    "Competitor '%s' has retired and cannot be reactivated or change status"
                            .formatted(competitor.getNickname()));
        }
        if (competitor.getStatus() == request.status()) {
            throw new ConflictException(
                    "Competitor '%s' already has status %s"
                            .formatted(competitor.getNickname(), request.status()));
        }

        competitor.setStatus(request.status());
        Competitor updated = competitorRepository.save(competitor);
        log.info("Competitor id={} status changed to {}", id, request.status());
        return CompetitorMapper.toResponse(updated);
    }

    /**
     * "Delete" a competitor. Per Module 2, one with official race results
     * cannot be physically deleted — it must be retired instead, preserving
     * its history. One with no official results has nothing to preserve, so
     * it's removed outright.
     *
     * <p>Idempotent for already-retired competitors that do have history:
     * calling this again just re-confirms RETIRED. A competitor that was
     * already physically deleted simply won't be found anymore.</p>
     */
    @Transactional
    public void retire(UUID id) {
        Competitor competitor = competitorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));

        if (hasOfficialResults(competitor)) {
            competitor.setStatus(CompetitorStatus.RETIRED);
            competitorRepository.save(competitor);
            log.info("Competitor retired id={} (has official results)", id);
            return;
        }

        competitorRepository.delete(competitor);
        log.info("Competitor permanently deleted id={} (no official results)", id);
    }

    /**
     * Whether this competitor has an official race history. Relies on
     * {@code completedRaces} rather than a separate query against results:
     * per the assignment, "updating a result must update statistics
     * consistently", so this counter is kept in sync with every officially
     * recorded result (win, loss, DNF or DSQ alike) and is a direct,
     * authoritative answer to "has this competitor officially raced?".
     */
    private boolean hasOfficialResults(Competitor competitor) {
        return competitor.getCompletedRaces() > 0;
    }

    @Transactional
    public CompetitorResponse patch(UUID id, CompetitorPatchRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("The request has no fields to update");
        }

        Competitor competitor = competitorRepository.findWithTeamById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));

        if (request.nickname() != null) {
            if (competitorRepository.existsByNicknameIgnoreCaseAndIdNot(request.nickname(), id)) {
                throw new ConflictException(
                        "There is already another competitor nicknamed '%s'".formatted(request.nickname()));
            }
            competitor.setNickname(request.nickname());
        }
        if (request.name() != null) {
            competitor.setName(request.name());
        }
        if (request.competitorType() != null) {
            competitor.setCompetitorType(request.competitorType());
        }
        if (request.dateOfBirth() != null) {
            competitor.setBirthDate(request.dateOfBirth());
        }
        if (request.approximateAge() != null) {
            competitor.setAge(request.approximateAge());
        }
        if (request.height() != null) {
            competitor.setHeight(request.height());
        }
        if (request.weight() != null) {
            competitor.setWeight(request.weight());
        }
        if (request.origin() != null) {
            competitor.setOrigin(request.origin());
        }

        Competitor updated = competitorRepository.save(competitor);
        log.info("Competitor patched id={}", id);
        return CompetitorMapper.toResponse(updated);
    }
}