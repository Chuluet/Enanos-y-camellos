package com.example.enanosycamellos.competitor.mapper;

import com.example.enanosycamellos.competitor.dto.CompetitorRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorResponse;
import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.team.mapper.TeamMapper;

import java.util.List;

/**
 * Translates between the {@link Competitor} entity and its DTOs.
 *
 * <p>This layer exists for the same reason as {@code CowMapper}: the entity
 * has a lazy relationship ({@code team}) and if the controller returned
 * the entity directly, Jackson would trigger a query outside the transaction
 * trying to serialize it.</p>
 *
 * <p>Utility class: private constructor and static methods, it's not a
 * Spring bean because it doesn't need to inject anything.</p>
 */
public final class CompetitorMapper {

    private CompetitorMapper() {
        // Utility class: not instantiated.
    }

    /**
     * Builds the entity with the competitor's own data.
     *
     * <p>Team is NOT resolved here: it's an entity that must be fetched
     * from the database, and the mapper doesn't have repositories.
     * {@code CompetitorService} takes care of that.</p>
     */
    public static Competitor toEntity(CompetitorRequest request) {
        if (request == null) return null;
        return Competitor.builder()
                .name(request.name())
                .nickname(request.nickname())
                .competitorType(request.competitorType())
                .birthDate(request.dateOfBirth())
                .age(request.approximateAge())
                .height(request.height())
                .weight(request.weight())
                .origin(request.origin())
                .status(request.statusOrDefault())
                .build();
    }

    /**
     * Converts the entity into the full response, including its team
     * in short version (to avoid recursion: competitor -> team -> members
     * -> team -> ...).
     *
     * <p>Warning: must be called <b>inside</b> the transaction, because here
     * {@code getTeam()} is accessed, which is LAZY. If called after the
     * transaction is closed, Hibernate would throw
     * LazyInitializationException.</p>
     */
    public static CompetitorResponse toResponse(Competitor competitor) {
        if (competitor == null) return null;
        return new CompetitorResponse(
                competitor.getId(),
                competitor.getName(),
                competitor.getNickname(),
                competitor.getCompetitorType(),
                competitor.getBirthDate(),
                competitor.getAge(),
                competitor.getHeight(),
                competitor.getWeight(),
                competitor.getOrigin(),
                competitor.getStatus(),
                competitor.getRegistrationDate(),
                competitor.getVictories(),
                competitor.getDefeats(),
                competitor.getCompletedRaces(),
                TeamMapper.toSummary(competitor.getTeam())
        );
    }

    /** Short version, for when the competitor appears inside a team. */
    public static CompetitorSummaryResponse toSummary(Competitor competitor) {
        if (competitor == null) return null;
        return new CompetitorSummaryResponse(
                competitor.getId(),
                competitor.getName(),
                competitor.getNickname(),
                competitor.getCompetitorType(),
                competitor.getStatus()
        );
    }

    /** Converts a full list of competitors into summaries. */
    public static List<CompetitorSummaryResponse> toSummaries(List<Competitor> competitors) {
        if (competitors == null) return List.of();
        return competitors.stream()
                .map(CompetitorMapper::toSummary)
                .toList();
    }
}