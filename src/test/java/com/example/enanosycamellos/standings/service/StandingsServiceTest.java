package com.example.enanosycamellos.standings.service;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.result.entity.RaceResult;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.result.repository.IRaceResultRepository;
import com.example.enanosycamellos.standings.dto.CompetitorStandingResponse;
import com.example.enanosycamellos.standings.dto.StandingsResponse;
import com.example.enanosycamellos.standings.dto.TeamStandingResponse;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingsServiceTest {

    @Mock
    private IRaceResultRepository resultRepository;

    @InjectMocks
    private StandingsService standingsService;

    // =========================== helper builders ============================

    private Race buildRace() {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Test Race")
                .scheduledDateTime(LocalDateTime.now().plusDays(1))
                .startLocation("Start")
                .finishLocation("Finish")
                .distanceMeters(1000)
                .maxParticipants(6)
                .raceType(RaceType.MIXED)
                .status(RaceStatus.IN_PROGRESS)
                .organizer("organizer")
                .registrationDeadline(LocalDateTime.now().minusHours(1))
                .build();
    }

    private Competitor buildCompetitor(String nickname, int victories, int defeats, int completedRaces) {
        return Competitor.builder()
                .id(UUID.randomUUID())
                .name(nickname)
                .nickname(nickname)
                .competitorType(CompetitorType.CAMEL)
                .height(1.9)
                .weight(300.0)
                .origin("Alto de Las Palmas")
                .status(CompetitorStatus.ACTIVE)
                .victories(victories)
                .defeats(defeats)
                .completedRaces(completedRaces)
                .build();
    }

    private Team buildTeam(String name, int victories, int defeats) {
        return Team.builder()
                .id(UUID.randomUUID())
                .name(name)
                .status(TeamStatus.ACTIVE)
                .coach("coach")
                .maxMembers(6)
                .victories(victories)
                .defeats(defeats)
                .build();
    }

    private RaceRegistration buildIndividualRegistration(Race race, Competitor competitor) {
        return RaceRegistration.builder()
                .id(UUID.randomUUID()).race(race).competitor(competitor)
                .status(RegistrationStatus.APPROVED).registeredBy("organizer")
                .build();
    }

    private RaceRegistration buildTeamRegistration(Race race, Team team) {
        return RaceRegistration.builder()
                .id(UUID.randomUUID()).race(race).team(team)
                .status(RegistrationStatus.APPROVED).registeredBy("organizer")
                .build();
    }

    private RaceResult buildResult(RaceRegistration registration, ResultStatus status, Integer finalPosition) {
        return RaceResult.builder()
                .id(UUID.randomUUID()).registration(registration).status(status)
                .finalPosition(finalPosition).penaltyTime(0.0)
                .recordedBy("organizer").recordedAt(LocalDateTime.now())
                .build();
    }

    // ============================ getCompetitorStandings() ============================

    @Test
    void getCompetitorStandings_shouldSumPointsAcrossMultipleRaces() {
        Race race1 = buildRace();
        Race race2 = buildRace();
        Competitor byte_ = buildCompetitor("Byte", 1, 0, 1);
        RaceRegistration reg1 = buildIndividualRegistration(race1, byte_);
        RaceRegistration reg2 = buildIndividualRegistration(race2, byte_);

        // Won race1 (10 points), came 3rd in race2 (5 points) = 15 total
        RaceResult result1 = buildResult(reg1, ResultStatus.FINISHED, 1);
        RaceResult result2 = buildResult(reg2, ResultStatus.FINISHED, 3);

        when(resultRepository.findAllByRegistration_CompetitorIsNotNull())
                .thenReturn(List.of(result1, result2));

        List<CompetitorStandingResponse> standings = standingsService.getCompetitorStandings();

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).totalPoints()).isEqualTo(15);
    }

    @Test
    void getCompetitorStandings_shouldGiveZeroPoints_forDisqualifiedOrDnf() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor("Tiny Docker", 0, 1, 1);
        RaceRegistration registration = buildIndividualRegistration(race, competitor);
        RaceResult result = buildResult(registration, ResultStatus.DID_NOT_FINISH, null);

        when(resultRepository.findAllByRegistration_CompetitorIsNotNull()).thenReturn(List.of(result));

        List<CompetitorStandingResponse> standings = standingsService.getCompetitorStandings();

        assertThat(standings.get(0).totalPoints()).isEqualTo(0);
    }

    @Test
    void getCompetitorStandings_shouldGiveZeroPoints_forPositionsBeyondFifth() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor("Last Place", 0, 1, 1);
        RaceRegistration registration = buildIndividualRegistration(race, competitor);
        RaceResult result = buildResult(registration, ResultStatus.FINISHED, 6);

        when(resultRepository.findAllByRegistration_CompetitorIsNotNull()).thenReturn(List.of(result));

        List<CompetitorStandingResponse> standings = standingsService.getCompetitorStandings();

        assertThat(standings.get(0).totalPoints()).isEqualTo(0);
    }

    @Test
    void getCompetitorStandings_shouldOrderByPointsDescending() {
        Race race = buildRace();
        Competitor first = buildCompetitor("Byte", 1, 0, 1);
        Competitor second = buildCompetitor("Null Pointer", 0, 1, 1);

        RaceResult winnerResult = buildResult(buildIndividualRegistration(race, first), ResultStatus.FINISHED, 1);
        RaceResult loserResult = buildResult(buildIndividualRegistration(race, second), ResultStatus.FINISHED, 5);

        when(resultRepository.findAllByRegistration_CompetitorIsNotNull())
                .thenReturn(List.of(loserResult, winnerResult)); // orden de entrada invertido a propósito

        List<CompetitorStandingResponse> standings = standingsService.getCompetitorStandings();

        assertThat(standings).hasSize(2);
        assertThat(standings.get(0).nickname()).isEqualTo("Byte");
        assertThat(standings.get(0).totalPoints()).isEqualTo(10);
        assertThat(standings.get(1).nickname()).isEqualTo("Null Pointer");
        assertThat(standings.get(1).totalPoints()).isEqualTo(1);
    }

    @Test
    void getCompetitorStandings_shouldIncludeVictoriesDefeatsAndCompletedRacesFromEntity() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor("Byte", 3, 2, 5);
        RaceRegistration registration = buildIndividualRegistration(race, competitor);
        RaceResult result = buildResult(registration, ResultStatus.FINISHED, 2);

        when(resultRepository.findAllByRegistration_CompetitorIsNotNull()).thenReturn(List.of(result));

        CompetitorStandingResponse standing = standingsService.getCompetitorStandings().get(0);

        assertThat(standing.victories()).isEqualTo(3);
        assertThat(standing.defeats()).isEqualTo(2);
        assertThat(standing.completedRaces()).isEqualTo(5);
    }

    @Test
    void getCompetitorStandings_shouldReturnEmptyList_whenNoResultsExist() {
        when(resultRepository.findAllByRegistration_CompetitorIsNotNull()).thenReturn(List.of());

        assertThat(standingsService.getCompetitorStandings()).isEmpty();
    }

    // ============================ getTeamStandings() ============================

    @Test
    void getTeamStandings_shouldSumPointsAcrossMultipleRaces() {
        Race race1 = buildRace();
        Race race2 = buildRace();
        Team team = buildTeam("The Five Exceptions", 1, 1);
        RaceRegistration reg1 = buildTeamRegistration(race1, team);
        RaceRegistration reg2 = buildTeamRegistration(race2, team);

        RaceResult result1 = buildResult(reg1, ResultStatus.FINISHED, 2); // 7 points
        RaceResult result2 = buildResult(reg2, ResultStatus.DISQUALIFIED, null); // 0 points

        when(resultRepository.findAllByRegistration_TeamIsNotNull()).thenReturn(List.of(result1, result2));

        List<TeamStandingResponse> standings = standingsService.getTeamStandings();

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).totalPoints()).isEqualTo(7);
        assertThat(standings.get(0).victories()).isEqualTo(1);
        assertThat(standings.get(0).defeats()).isEqualTo(1);
    }

    @Test
    void getTeamStandings_shouldOrderByPointsDescending() {
        Race race = buildRace();
        Team leading = buildTeam("The Five Exceptions", 1, 0);
        Team trailing = buildTeam("Stray Camels", 0, 1);

        RaceResult leadingResult = buildResult(buildTeamRegistration(race, leading), ResultStatus.FINISHED, 1);
        RaceResult trailingResult = buildResult(buildTeamRegistration(race, trailing), ResultStatus.FINISHED, 4);

        when(resultRepository.findAllByRegistration_TeamIsNotNull())
                .thenReturn(List.of(trailingResult, leadingResult));

        List<TeamStandingResponse> standings = standingsService.getTeamStandings();

        assertThat(standings.get(0).name()).isEqualTo("The Five Exceptions");
        assertThat(standings.get(1).name()).isEqualTo("Stray Camels");
    }

    @Test
    void getTeamStandings_shouldReturnEmptyList_whenNoResultsExist() {
        when(resultRepository.findAllByRegistration_TeamIsNotNull()).thenReturn(List.of());

        assertThat(standingsService.getTeamStandings()).isEmpty();
    }

    // ============================ getStandings() ============================

    @Test
    void getStandings_shouldCombineCompetitorAndTeamStandings() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor("Byte", 1, 0, 1);
        Team team = buildTeam("The Five Exceptions", 1, 0);

        RaceResult competitorResult =
                buildResult(buildIndividualRegistration(race, competitor), ResultStatus.FINISHED, 1);
        RaceResult teamResult =
                buildResult(buildTeamRegistration(race, team), ResultStatus.FINISHED, 1);

        when(resultRepository.findAllByRegistration_CompetitorIsNotNull()).thenReturn(List.of(competitorResult));
        when(resultRepository.findAllByRegistration_TeamIsNotNull()).thenReturn(List.of(teamResult));

        StandingsResponse standings = standingsService.getStandings();

        assertThat(standings.competitors()).hasSize(1);
        assertThat(standings.teams()).hasSize(1);
    }
}