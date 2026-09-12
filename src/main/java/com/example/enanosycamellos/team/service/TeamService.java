package com.example.enanosycamellos.team.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.team.dto.TeamPatchRequest;
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

    /**
     * Team listing, optionally filtered by status.
     *
     * <p>No pagination here: unlike Module 2 (Competitors), the assignment
     * only requires filtering/pagination/sorting for competitors, so teams
     * stay as a plain list. Both branches fetch-join members in a single
     * query, avoiding N+1 when mapping each team's member list. Without a
     * filter, non-inactive teams are returned (same default as before);
     * with a filter, an exact-status match is used instead.</p>
     */
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeams(TeamStatus status) {
        List<Team> teams = status != null
                ? teamRepository.findAllWithMembers(status)
                : teamRepository.findAllByStatusNotWithMembers(TeamStatus.INACTIVE);

        return teams.stream()
                .map(TeamMapper::toResponse)
                .toList();
    }

    /** A team by id, with members. Throws 404 if not found. */
    @Transactional(readOnly = true)
    public TeamResponse getById(UUID id) {
        Team team = teamRepository.findWithMembersById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));

        if (team.getStatus() == TeamStatus.INACTIVE) {
            throw ResourceNotFoundException.of("Team", id);
        }

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
     * Full replace via PUT. Every editable field is mandatory (enforced by
     * validation on {@link TeamUpdateRequest}), so there is no "isEmpty"
     * check here — unlike {@link #patch}, a PUT with nothing to change
     * doesn't make sense as a request. Members and statistics are untouched:
     * members have their own endpoints, and the statistics belong to the
     * Results module.
     */
    @Transactional
    public TeamResponse update(UUID id, TeamUpdateRequest request) {
        Team team = teamRepository.findWithMembersById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));

        if (teamRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException(
                    "There is already another team named '%s'".formatted(request.name()));
        }
        if (request.maxMembers() < team.getMembers().size()) {
            throw new ConflictException(
                    "maxMembers (%d) cannot be less than the current member count (%d)"
                            .formatted(request.maxMembers(), team.getMembers().size()));
        }

        team.setName(request.name());
        team.setDescription(request.description());
        team.setCoach(request.coach());
        team.setMaxMembers(request.maxMembers());
        team.setStatus(request.status());

        Team updated = teamRepository.save(team);
        log.info("Team updated (PUT) id={}", id);
        return TeamMapper.toResponse(updated);
    }

    /**
     * Partially updates the team (PATCH semantics): only fields sent
     * (non-null) are changed. Complements {@link #update} (PUT, full
     * replace). Members and statistics are not touched here either.
     */
    @Transactional
    public TeamResponse patch(UUID id, TeamPatchRequest request) {
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
        log.info("Team patched id={}", id);
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

        if (competitor.getStatus() == CompetitorStatus.RETIRED) {
            throw new ConflictException(
                    "Competitor '%s' is retired and cannot join a team".formatted(competitor.getNickname()));
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
     * "Delete" a team. Per the assignment, one with official race history
     * cannot be physically deleted — it must be deactivated instead, keeping
     * that history. One with no race history has nothing to preserve, so
     * it's removed outright; its members are detached first so each
     * competitor's {@code team_id} is cleared before the team row goes away.
     */
    @Transactional
    public void deactivate(UUID id) {
        Team team = teamRepository.findWithMembersById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));

        if (hasOfficialRaceHistory(team)) {
            team.setStatus(TeamStatus.INACTIVE);
            teamRepository.save(team);
            log.info("Team deactivated id={} (has official race history)", id);
            return;
        }

        for (Competitor member : List.copyOf(team.getMembers())) {
            team.removeMember(member);
            competitorRepository.save(member);
        }
        teamRepository.delete(team);
        log.info("Team permanently deleted id={} (no official race history)", id);
    }

    /**
     * Whether this team has official race history. Relies on
     * {@code victories}/{@code defeats} rather than a separate query
     * against results, same reasoning as {@code CompetitorService}. Note:
     * unlike {@code Competitor}, {@code Team} has no {@code completedRaces}
     * counter, so a team whose only official results were DNF/DSQ (neither
     * a win nor a loss) would read as having no history here. If that edge
     * case matters, add a {@code completedRaces} counter to {@code Team}
     * (mirroring {@code Competitor}) and check that instead.
     */
    private boolean hasOfficialRaceHistory(Team team) {
        return team.getVictories() > 0 || team.getDefeats() > 0;
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

            if (competitor.getStatus() == CompetitorStatus.RETIRED) {
                throw new ConflictException(
                        "Competitor '%s' is retired and cannot join a team".formatted(competitor.getNickname()));
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