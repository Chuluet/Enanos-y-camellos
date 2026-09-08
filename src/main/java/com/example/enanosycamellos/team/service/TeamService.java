package com.example.enanosycamellos.team.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.team.dto.TeamRequest;
import com.example.enanosycamellos.team.dto.TeamResponse;
import com.example.enanosycamellos.team.dto.TeamUpdateRequest;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.example.enanosycamellos.team.mapper.TeamMapper;
import com.example.enanosycamellos.team.repository.ITeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Team business rules.
 *
 * <p>Unlike {@code CowService}, there is no separate owner service to inject:
 * {@code Team} owns its members directly ({@code Competitor} is the owning
 * side of the relationship, {@code Team} is the inverse side). That's why
 * {@code ICompetitorRepository} is injected here directly, the same way
 * {@code IClownRepository} is injected in {@code CowService} to assign
 * clowns.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final ITeamRepository teamRepository;
    private final ICompetitorRepository competitorRepository;

    // ============================== Read ===============================

    /** All non-inactive teams, with members. */
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeams() {
        return teamRepository.findAllActiveWithMembers(TeamStatus.INACTIVE)
                .stream()
                .map(TeamMapper::toResponse)
                .toList();
    }

    /** A team by id, with members. Throws 404 if not found. */
    @Transactional(readOnly = true)
    public TeamResponse getById(UUID id) {
        Team team = teamRepository.findWithMembersById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));
        return TeamMapper.toResponse(team);
    }

    // ============================= Write ==============================

    /**
     * Creates a team and, optionally, assigns it initial members.
     *
     * <p>The team is saved <b>before</b> assigning members, same reasoning as
     * {@code CowService.create()}: the members need the team to already
     * exist so the {@code team_id} FK on {@code competitors} has something
     * valid to point to.</p>
     */
    @Transactional
    public TeamResponse create(TeamRequest request) {
        if (teamRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException(
                    "There is already a team named '%s'".formatted(request.name()));
        }

        Team team = TeamMapper.toEntity(request);
        Team saved = teamRepository.save(team);

        assignMembers(saved, request.memberIdsOrEmpty());

        log.info("Team created id={} members={}", saved.getId(), request.memberIdsOrEmpty().size());
        return TeamMapper.toResponse(saved);
    }

    /**
     * Updates the team's own data. Fields in null are not touched
     * (PATCH semantics). Members have separate endpoints.
     */
    @Transactional
    public TeamResponse update(UUID id, TeamUpdateRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("The request has no fields to update");
        }

        Team team = teamRepository.findWithMembersById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));

        if (request.name() != null) {
            if (teamRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
                throw new ConflictException(
                        "There is already another team named '%s'".formatted(request.name()));
            }
            team.setName(request.name());
        }
        if (request.description() != null) {
            team.setDescription(request.description());
        }
        if (request.coach() != null) {
            team.setCoach(request.coach());
        }
        if (request.maxMembers() != null) {
            // Can't shrink capacity below the members already assigned.
            if (request.maxMembers() < team.getMembers().size()) {
                throw new ConflictException(
                        "maxMembers (%d) cannot be less than the current member count (%d)"
                                .formatted(request.maxMembers(), team.getMembers().size()));
            }
            team.setMaxMembers(request.maxMembers());
        }
        if (request.status() != null) {
            team.setStatus(request.status());
        }

        Team updated = teamRepository.save(team);
        log.info("Team updated id={}", id);
        return TeamMapper.toResponse(updated);
    }

    /**
     * Adds a competitor to the team (N to 1 insertion on an already existing
     * team, mirrors {@code CowService.changeOwner}).
     */
    @Transactional
    public TeamResponse addMember(UUID teamId, UUID competitorId) {
        Team team = teamRepository.findWithMembersById(teamId)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", teamId));

        if (team.getStatus() == TeamStatus.INACTIVE) {
            throw new ConflictException("Cannot add members to an inactive team");
        }
        if (team.getMembers().size() >= team.getMaxMembers()) {
            throw new ConflictException(
                    "Team '%s' already has the maximum number of members (%d)"
                            .formatted(team.getName(), team.getMaxMembers()));
        }

        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", competitorId));

        if (competitor.getStatus() != CompetitorStatus.ACTIVE) {
            throw new BadRequestException(
                    "Competitor '%s' is not active".formatted(competitor.getNickname()));
        }
        if (competitor.getTeam() != null) {
            throw new ConflictException(
                    "Competitor '%s' already belongs to a team".formatted(competitor.getNickname()));
        }

        team.addMember(competitor);
        competitorRepository.save(competitor);

        log.info("Competitor id={} added to team id={}", competitorId, teamId);
        return TeamMapper.toResponse(team);
    }

    /** Removes a competitor from the team, leaving it without a team (not deleted). */
    @Transactional
    public TeamResponse removeMember(UUID teamId, UUID competitorId) {
        Team team = teamRepository.findWithMembersById(teamId)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", teamId));

        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", competitorId));

        if (!team.hasMember(competitor)) {
            throw new ResourceNotFoundException(
                    "Competitor '%s' does not belong to team '%s'"
                            .formatted(competitor.getNickname(), team.getName()));
        }

        team.removeMember(competitor);
        competitorRepository.save(competitor);

        log.info("Competitor id={} removed from team id={}", competitorId, teamId);
        return TeamMapper.toResponse(team);
    }

    /**
     * "Delete" a team: per the assignment, a team with race history cannot be
     * physically deleted, only deactivated. Since the Race module doesn't
     * exist yet, this currently deactivates unconditionally — once
     * {@code RaceRegistration} exists, add a check here before allowing it.
     */
    @Transactional
    public void deactivate(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));

        team.setStatus(TeamStatus.INACTIVE);
        teamRepository.save(team);
        log.info("Team deactivated id={}", id);
    }

    // ============================= Utilities ============================

    /** Assigns each competitor id to the team, validating eligibility. */
    private void assignMembers(Team team, List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }
        if (memberIds.size() > team.getMaxMembers()) {
            throw new BadRequestException(
                    "Cannot assign %d members: team max is %d"
                            .formatted(memberIds.size(), team.getMaxMembers()));
        }

        for (UUID competitorId : memberIds) {
            Competitor competitor = competitorRepository.findById(competitorId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Competitor", competitorId));

            if (competitor.getStatus() != CompetitorStatus.ACTIVE) {
                throw new BadRequestException(
                        "Competitor '%s' is not active".formatted(competitor.getNickname()));
            }
            if (competitor.getTeam() != null) {
                throw new ConflictException(
                        "Competitor '%s' already belongs to a team".formatted(competitor.getNickname()));
            }

            // Ignores duplicates within the same request instead of failing.
            if (!team.hasMember(competitor)) {
                team.addMember(competitor);
                competitorRepository.save(competitor);
            }
        }
    }
}