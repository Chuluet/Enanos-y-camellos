package com.example.enanosycamellos.registration.mapper;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the registration mapper. Pure tests: no Spring, no mocks —
 * same approach as TeamMapperTest.
 */
class RegistrationMapperTest {

    private Race buildRace() {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Byte's Revenge 1K")
                .scheduledDateTime(LocalDateTime.now().plusDays(5))
                .startLocation("Start")
                .finishLocation("Finish")
                .distanceMeters(1000)
                .maxParticipants(6)
                .raceType(RaceType.MIXED)
                .status(RaceStatus.OPEN_FOR_REGISTRATION)
                .organizer("organizer")
                .registrationDeadline(LocalDateTime.now().plusDays(3))
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

    // ============================== toEntity ==============================

    @Test
    @DisplayName("toEntity builds an individual registration in PENDING status")
    void toEntity_buildsIndividualRegistration() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor();

        RaceRegistration registration =
                RegistrationMapper.toEntity(race, competitor, null, 2, "organizer");

        assertSame(race, registration.getRace());
        assertSame(competitor, registration.getCompetitor());
        assertNull(registration.getTeam());
        assertEquals(2, registration.getStartingPosition());
        assertEquals("organizer", registration.getRegisteredBy());
        assertEquals(RegistrationStatus.PENDING, registration.getStatus());
    }

    @Test
    @DisplayName("toEntity builds a team registration in PENDING status")
    void toEntity_buildsTeamRegistration() {
        Race race = buildRace();
        Team team = buildTeam();

        RaceRegistration registration =
                RegistrationMapper.toEntity(race, null, team, null, "organizer");

        assertNull(registration.getCompetitor());
        assertSame(team, registration.getTeam());
        assertEquals(RegistrationStatus.PENDING, registration.getStatus());
    }

    // ============================== toResponse ==============================

    @Test
    @DisplayName("toResponse maps an individual registration, with team null")
    void toResponse_mapsIndividualRegistration() {
        Race race = buildRace();
        Competitor competitor = buildCompetitor();
        RaceRegistration registration = RaceRegistration.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(competitor)
                .status(RegistrationStatus.PENDING)
                .registrationDate(LocalDateTime.now())
                .registeredBy("organizer")
                .build();

        RaceRegistrationResponse response = RegistrationMapper.toResponse(registration);

        assertEquals(registration.getId(), response.id());
        assertEquals(race.getId(), response.race().id());
        assertNotNull(response.competitor());
        assertEquals(competitor.getId(), response.competitor().id());
        assertNull(response.team());
        assertEquals(RegistrationStatus.PENDING, response.status());
    }

    @Test
    @DisplayName("toResponse maps a team registration, with competitor null")
    void toResponse_mapsTeamRegistration() {
        Race race = buildRace();
        Team team = buildTeam();
        RaceRegistration registration = RaceRegistration.builder()
                .id(UUID.randomUUID())
                .race(race)
                .team(team)
                .status(RegistrationStatus.APPROVED)
                .registrationDate(LocalDateTime.now())
                .registeredBy("organizer")
                .build();

        RaceRegistrationResponse response = RegistrationMapper.toResponse(registration);

        assertNull(response.competitor());
        assertNotNull(response.team());
        assertEquals(team.getId(), response.team().id());
    }

    @Test
    @DisplayName("toResponse with null returns null")
    void toResponse_withNull_returnsNull() {
        assertNull(RegistrationMapper.toResponse(null));
    }

    // ============================== toResponseList ==============================

    @Test
    @DisplayName("toResponseList maps every element in order")
    void toResponseList_mapsAllElements() {
        Race race = buildRace();
        RaceRegistration r1 = RaceRegistration.builder()
                .id(UUID.randomUUID()).race(race).competitor(buildCompetitor())
                .status(RegistrationStatus.PENDING).registrationDate(LocalDateTime.now())
                .registeredBy("organizer").build();
        RaceRegistration r2 = RaceRegistration.builder()
                .id(UUID.randomUUID()).race(race).team(buildTeam())
                .status(RegistrationStatus.APPROVED).registrationDate(LocalDateTime.now())
                .registeredBy("organizer").build();

        List<RaceRegistrationResponse> result = RegistrationMapper.toResponseList(List.of(r1, r2));

        assertEquals(2, result.size());
        assertEquals(r1.getId(), result.get(0).id());
        assertEquals(r2.getId(), result.get(1).id());
    }

    @Test
    @DisplayName("toResponseList with null returns an empty list")
    void toResponseList_withNull_returnsEmptyList() {
        assertTrue(RegistrationMapper.toResponseList(null).isEmpty());
    }
}