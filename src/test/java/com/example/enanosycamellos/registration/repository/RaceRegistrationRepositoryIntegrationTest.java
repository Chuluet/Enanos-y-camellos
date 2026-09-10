package com.example.enanosycamellos.registration.repository;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RaceRegistrationRepositoryIntegrationTest {

    @Autowired
    private IRaceRegistrationRepository registrationRepository;

    @Autowired
    private TestEntityManager em;

    private Race race;
    private Competitor competitor;
    private Team team;

    @BeforeEach
    void setUp() {
        race = Race.builder()
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
        em.persistAndFlush(race);

        team = Team.builder()
                .name("The Five Exceptions").coach("coach")
                .status(TeamStatus.ACTIVE).maxMembers(6)
                .build();
        em.persistAndFlush(team);

        competitor = Competitor.builder()
                .name("Byte").nickname("byte-nick")
                .competitorType(CompetitorType.CAMEL)
                .height(1.9).weight(300.0).origin("Alto de Las Palmas")
                .status(CompetitorStatus.ACTIVE)
                .build();
        em.persistAndFlush(competitor);
    }

    private RaceRegistration persistRegistration(Competitor competitor, Team team,
                                                  RegistrationStatus status, Integer startingPosition) {
        RaceRegistration registration = RaceRegistration.builder()
                .race(race)
                .competitor(competitor)
                .team(team)
                .status(status)
                .startingPosition(startingPosition)
                .registeredBy("organizer")
                .build();
        return em.persistAndFlush(registration);
    }

    @Test
    @DisplayName("findAllByRace_Id returns every registration for that race")
    void findAllByRaceId_returnsRegistrationsForThatRace() {
        persistRegistration(competitor, null, RegistrationStatus.PENDING, 1);

        List<RaceRegistration> result = registrationRepository.findAllByRace_Id(race.getId());

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("existsByRace_IdAndCompetitor_IdAndStatusIn detects an active registration")
    void existsByRaceIdAndCompetitorIdAndStatusIn_detectsActiveRegistration() {
        persistRegistration(competitor, null, RegistrationStatus.APPROVED, null);

        boolean exists = registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(
                race.getId(), competitor.getId(), Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByRace_IdAndCompetitor_IdAndStatusIn ignores CANCELLED registrations")
    void existsByRaceIdAndCompetitorIdAndStatusIn_ignoresCancelledRegistration() {
        persistRegistration(competitor, null, RegistrationStatus.CANCELLED, null);

        boolean exists = registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(
                race.getId(), competitor.getId(), Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));

        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByRace_IdAndTeam_IdAndStatusIn detects an active team registration")
    void existsByRaceIdAndTeamIdAndStatusIn_detectsActiveRegistration() {
        persistRegistration(null, team, RegistrationStatus.PENDING, null);

        boolean exists = registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(
                race.getId(), team.getId(), Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByRace_IdAndCompetitor_IdInAndStatusIn detects a member already registered individually")
    void existsByRaceIdAndCompetitorIdInAndStatusIn_detectsMemberRegistration() {
        persistRegistration(competitor, null, RegistrationStatus.APPROVED, null);

        boolean exists = registrationRepository.existsByRace_IdAndCompetitor_IdInAndStatusIn(
                race.getId(), List.of(competitor.getId()),
                Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByRace_IdAndStartingPositionAndStatusIn detects a taken starting position")
    void existsByRaceIdAndStartingPositionAndStatusIn_detectsTakenPosition() {
        persistRegistration(competitor, null, RegistrationStatus.APPROVED, 3);

        boolean exists = registrationRepository.existsByRace_IdAndStartingPositionAndStatusIn(
                race.getId(), 3, Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));

        assertTrue(exists);
    }

    @Test
    @DisplayName("countByRace_IdAndStatus counts only registrations in that exact status")
    void countByRaceIdAndStatus_countsOnlyMatchingStatus() {
        persistRegistration(competitor, null, RegistrationStatus.APPROVED, 1);

        Competitor secondCompetitor = Competitor.builder()
                .name("Stack Overflow").nickname("stacky")
                .competitorType(CompetitorType.DWARF)
                .height(1.1).weight(60.0).origin("Alto de Las Palmas")
                .status(CompetitorStatus.ACTIVE)
                .build();
        em.persistAndFlush(secondCompetitor);
        persistRegistration(secondCompetitor, null, RegistrationStatus.PENDING, 2);

        long approvedCount = registrationRepository.countByRace_IdAndStatus(
                race.getId(), RegistrationStatus.APPROVED);

        assertEquals(1L, approvedCount);
    }
}