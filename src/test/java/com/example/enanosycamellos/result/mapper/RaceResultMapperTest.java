package com.example.enanosycamellos.result.mapper;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.result.dto.RaceResultResponse;
import com.example.enanosycamellos.result.entity.RaceResult;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RaceResultMapperTest {

    private Race buildRace() {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Byte's Revenge 1K")
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

    private Competitor buildCompetitor() {
        return Competitor.builder()
                .id(UUID.randomUUID())
                .name("Byte")
                .nickname("byte-nick")
                .competitorType(CompetitorType.CAMEL)
                .height(1.9)
                .weight(300.0)
                .origin("Alto de Las Palmas")
                .status(CompetitorStatus.ACTIVE)
                .build();
    }

    private Team buildTeam() {
        return Team.builder()
                .id(UUID.randomUUID())
                .name("The Five Exceptions")
                .status(TeamStatus.ACTIVE)
                .coach("coach")
                .maxMembers(6)
                .build();
    }

    private RaceRegistration buildIndividualRegistration(Race race, Competitor competitor) {
        return RaceRegistration.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(competitor)
                .status(RegistrationStatus.APPROVED)
                .startingPosition(1)
                .registeredBy("organizer")
                .registrationDate(LocalDateTime.now())
                .build();
    }

    private RaceRegistration buildTeamRegistration(Race race, Team team) {
        return RaceRegistration.builder()
                .id(UUID.randomUUID())
                .race(race)
                .team(team)
                .status(RegistrationStatus.APPROVED)
                .registeredBy("organizer")
                .registrationDate(LocalDateTime.now())
                .build();
    }

    // ============================== toEntity ==============================

    @Test
    @DisplayName("toEntity maps every field and defaults penaltyTime to 0 when null")
    void toEntity_mapsFieldsAndDefaultsPenaltyTime() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor();
        RaceRegistration registration = buildIndividualRegistration(race, competitor);

        RaceResult result = RaceResultMapper.toEntity(
                registration, 2, 1, 120.5, null, ResultStatus.FINISHED, "great race", "organizer");

        assertSame(registration, result.getRegistration());
        assertEquals(2, result.getStartPosition());
        assertEquals(1, result.getFinalPosition());
        assertEquals(120.5, result.getCompletionTime());
        assertEquals(0.0, result.getPenaltyTime());
        assertEquals(ResultStatus.FINISHED, result.getStatus());
        assertEquals("great race", result.getNotes());
        assertEquals("organizer", result.getRecordedBy());
    }

    @Test
    @DisplayName("toEntity keeps an explicit penaltyTime instead of defaulting it")
    void toEntity_keepsExplicitPenaltyTime() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor();
        RaceRegistration registration = buildIndividualRegistration(race, competitor);

        RaceResult result = RaceResultMapper.toEntity(
                registration, null, 2, 130.0, 5.0, ResultStatus.FINISHED, null, "organizer");

        assertEquals(5.0, result.getPenaltyTime());
    }

    // ============================== toResponse ==============================

    @Test
    @DisplayName("toResponse maps an individual result, with team null")
    void toResponse_mapsIndividualResult() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor();
        RaceRegistration registration = buildIndividualRegistration(race, competitor);
        RaceResult result = RaceResult.builder()
                .id(UUID.randomUUID())
                .registration(registration)
                .startPosition(1)
                .finalPosition(1)
                .completionTime(115.0)
                .penaltyTime(0.0)
                .status(ResultStatus.FINISHED)
                .recordedBy("organizer")
                .recordedAt(LocalDateTime.now())
                .build();

        RaceResultResponse response = RaceResultMapper.toResponse(result);

        assertEquals(result.getId(), response.id());
        assertEquals(registration.getId(), response.registrationId());
        assertEquals(race.getId(), response.race().id());
        assertNotNull(response.competitor());
        assertEquals(competitor.getId(), response.competitor().id());
        assertNull(response.team());
        assertEquals(1, response.finalPosition());
    }

    @Test
    @DisplayName("toResponse maps a team result, with competitor null")
    void toResponse_mapsTeamResult() {
        Race race = buildRace();
        Team team = buildTeam();
        RaceRegistration registration = buildTeamRegistration(race, team);
        RaceResult result = RaceResult.builder()
                .id(UUID.randomUUID())
                .registration(registration)
                .status(ResultStatus.DISQUALIFIED)
                .penaltyTime(0.0)
                .recordedBy("organizer")
                .recordedAt(LocalDateTime.now())
                .build();

        RaceResultResponse response = RaceResultMapper.toResponse(result);

        assertNull(response.competitor());
        assertNotNull(response.team());
        assertEquals(team.getId(), response.team().id());
        assertEquals(ResultStatus.DISQUALIFIED, response.status());
    }

    @Test
    @DisplayName("toResponse with null returns null")
    void toResponse_withNull_returnsNull() {
        assertNull(RaceResultMapper.toResponse(null));
    }

    // ============================== toResponseList ==============================

    @Test
    @DisplayName("toResponseList maps every element in order")
    void toResponseList_mapsAllElements() {
        Race race = buildRace();
        RaceRegistration r1 = buildIndividualRegistration(race, buildCompetitor());
        RaceRegistration r2 = buildTeamRegistration(race, buildTeam());

        RaceResult result1 = RaceResult.builder()
                .id(UUID.randomUUID()).registration(r1).status(ResultStatus.FINISHED)
                .finalPosition(1).penaltyTime(0.0).recordedBy("organizer").recordedAt(LocalDateTime.now()).build();
        RaceResult result2 = RaceResult.builder()
                .id(UUID.randomUUID()).registration(r2).status(ResultStatus.DID_NOT_FINISH)
                .penaltyTime(0.0).recordedBy("organizer").recordedAt(LocalDateTime.now()).build();

        List<RaceResultResponse> result = RaceResultMapper.toResponseList(List.of(result1, result2));

        assertEquals(2, result.size());
        assertEquals(result1.getId(), result.get(0).id());
        assertEquals(result2.getId(), result.get(1).id());
    }

    @Test
    @DisplayName("toResponseList with null returns an empty list")
    void toResponseList_withNull_returnsEmptyList() {
        assertTrue(RaceResultMapper.toResponseList(null).isEmpty());
    }
}