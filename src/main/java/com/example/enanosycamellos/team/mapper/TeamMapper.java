package com.example.enanosycamellos.team.mapper;

import com.example.enanosycamellos.competitor.mapper.CompetitorMapper;
import com.example.enanosycamellos.team.dto.TeamRequest;
import com.example.enanosycamellos.team.dto.TeamResponse;
import com.example.enanosycamellos.team.dto.TeamSummaryResponse;
import com.example.enanosycamellos.team.entity.Team;

import java.util.List;

/**
 * Translates between the {@link Team} entity and its DTOs.
 *
 * <p>This layer exists for the same reason as {@code CowMapper}: the entity
 * has a lazy relationship ({@code members}) and if the controller returned
 * the entity directly, Jackson would trigger a query outside the transaction
 * trying to serialize it.</p>
 *
 * <p>Utility class: private constructor and static methods, it's not a
 * Spring bean because it doesn't need to inject anything.</p>
 */
public final class TeamMapper {

    private TeamMapper() {
        // Utility class: not instantiated.
    }

    /**
     * Builds the entity with the team's own data.
     *
     * <p>Members are NOT resolved here: they are entities that must be
     * fetched from the database, and the mapper doesn't have repositories.
     * {@code TeamService} takes care of that.</p>
     */
    public static Team toEntity(TeamRequest request) {
        if (request == null) return null;
        return Team.builder()
                .name(request.name())
                .description(request.description())
                .coach(request.coach())
                .maxMembers(request.maxMembers())
                .status(request.statusOrDefault())
                .build();
    }

    /**
     * Converts the entity into the full response.
     *
     * <p>Warning: must be called <b>inside</b> the transaction, because here
     * {@code getMembers()} is accessed, which is LAZY. If called after
     * closed, Hibernate would throw LazyInitializationException.</p>
     */
    public static TeamResponse toResponse(Team team) {
        if (team == null) return null;
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCoach(),
                team.getCreationDate(),
                team.getStatus(),
                team.getVictories(),
                team.getDefeats(),
                team.getMaxMembers(),
                team.getMembers().stream()
                        .map(CompetitorMapper::toSummary)
                        .toList()
        );
    }

    /** Short version, for when the team appears inside a competitor. */
    public static TeamSummaryResponse toSummary(Team team) {
        if (team == null) return null;
        return new TeamSummaryResponse(
                team.getId(),
                team.getName(),
                team.getStatus()
        );
    }

    /** Converts a full list of teams into summaries. */
    public static List<TeamSummaryResponse> toSummaries(List<Team> teams) {
        if (teams == null) return List.of();
        return teams.stream()
                .map(TeamMapper::toSummary)
                .toList();
    }
}