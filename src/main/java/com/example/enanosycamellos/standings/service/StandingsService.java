package com.example.enanosycamellos.standings.service;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.result.entity.RaceResult;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.result.repository.IRaceResultRepository;
import com.example.enanosycamellos.standings.dto.CompetitorStandingResponse;
import com.example.enanosycamellos.standings.dto.StandingsResponse;
import com.example.enanosycamellos.standings.dto.TeamStandingResponse;
import com.example.enanosycamellos.team.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Computes league standings on the fly from recorded results — no stored
 * "total points" column to keep in sync, this always reflects exactly what's
 * in the database right now.
 *
 * <p>victories/defeats/completedRaces come straight from the Competitor/Team
 * entities (kept up to date by {@code RaceResultService.applyStatistics});
 * only {@code totalPoints} is computed here, from the points-by-position
 * table in section 6 of the assignment.</p>
 */
@Service
@RequiredArgsConstructor
public class StandingsService {

    private static final Map<Integer, Integer> POINTS_BY_POSITION = Map.of(
            1, 10,
            2, 7,
            3, 5,
            4, 3,
            5, 1
    );

    private final IRaceResultRepository resultRepository;

    @Transactional(readOnly = true)
    public StandingsResponse getStandings() {
        return new StandingsResponse(getCompetitorStandings(), getTeamStandings());
    }

    @Transactional(readOnly = true)
    public List<CompetitorStandingResponse> getCompetitorStandings() {
        List<RaceResult> results = resultRepository.findAllByRegistration_CompetitorIsNotNull();

        Map<UUID, Competitor> competitorsById = new LinkedHashMap<>();
        Map<UUID, Integer> pointsById = new HashMap<>();

        for (RaceResult result : results) {
            Competitor competitor = result.getRegistration().getCompetitor();
            competitorsById.putIfAbsent(competitor.getId(), competitor);
            pointsById.merge(competitor.getId(), pointsFor(result), Integer::sum);
        }

        return competitorsById.values().stream()
                .map(c -> new CompetitorStandingResponse(
                        c.getId(),
                        c.getNickname(),
                        c.getCompetitorType(),
                        pointsById.getOrDefault(c.getId(), 0),
                        c.getVictories(),
                        c.getDefeats(),
                        c.getCompletedRaces()))
                .sorted(Comparator.comparingInt(CompetitorStandingResponse::totalPoints).reversed()
                        .thenComparing(CompetitorStandingResponse::nickname))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamStandingResponse> getTeamStandings() {
        List<RaceResult> results = resultRepository.findAllByRegistration_TeamIsNotNull();

        Map<UUID, Team> teamsById = new LinkedHashMap<>();
        Map<UUID, Integer> pointsById = new HashMap<>();

        for (RaceResult result : results) {
            Team team = result.getRegistration().getTeam();
            teamsById.putIfAbsent(team.getId(), team);
            pointsById.merge(team.getId(), pointsFor(result), Integer::sum);
        }

        return teamsById.values().stream()
                .map(t -> new TeamStandingResponse(
                        t.getId(),
                        t.getName(),
                        pointsById.getOrDefault(t.getId(), 0),
                        t.getVictories(),
                        t.getDefeats()))
                .sorted(Comparator.comparingInt(TeamStandingResponse::totalPoints).reversed()
                        .thenComparing(TeamStandingResponse::name))
                .toList();
    }

    private int pointsFor(RaceResult result) {
        if (result.getStatus() != ResultStatus.FINISHED || result.getFinalPosition() == null) {
            return 0;
        }
        return POINTS_BY_POSITION.getOrDefault(result.getFinalPosition(), 0);
    }
}