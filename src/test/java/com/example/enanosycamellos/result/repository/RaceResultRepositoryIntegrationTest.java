package com.example.enanosycamellos.result.repository;

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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RaceResultRepositoryIntegrationTest {

    @Autowired
    private IRaceResultRepository resultRepository;

    @Autowired
    private TestEntityManager em;

    private Race race;
    private Competitor competitor;
    private Team team;

    @BeforeEach
    void setUp() {
        race = Race.builder()
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

    private RaceRegistration persistIndividualRegistration() {
        RaceRegistration registration = RaceRegistration.builder()
                .race(race).competitor(competitor).status(RegistrationStatus.APPROVED)
                .startingPosition(1).registeredBy("organizer")
                .build();
        return em.persistAndFlush(registration);
    }

    private RaceRegistration persistTeamRegistration() {
        RaceRegistration registration = RaceRegistration.builder()
                .race(race).team(team).status(RegistrationStatus.APPROVED)
                .registeredBy("organizer")
                .build();
        return em.persistAndFlush(registration);
    }

    private RaceResult persistResult(RaceRegistration registration, ResultStatus status, Integer finalPosition) {
        RaceResult result = RaceResult.builder()
                .registration(registration)
                .status(status)
                .finalPosition(finalPosition)
                .completionTime(status == ResultStatus.FINISHED ? 120.0 : null)
                .penaltyTime(0.0)
                .recordedBy("organizer")
                .build();
        return em.persistAndFlush(result);
    }

    @Test
    @DisplayName("findAllByRegistration_Race_Id returns every result for that race")
    void findAllByRaceId_returnsResultsForThatRace() {
        RaceRegistration registration = persistIndividualRegistration();
        persistResult(registration, ResultStatus.FINISHED, 1);

        List<RaceResult> results = resultRepository.findAllByRegistration_Race_Id(race.getId());

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("findByRegistration_Id finds the result for that registration")
    void findByRegistrationId_findsResult() {
        RaceRegistration registration = persistIndividualRegistration();
        RaceResult saved = persistResult(registration, ResultStatus.FINISHED, 1);

        Optional<RaceResult> found = resultRepository.findByRegistration_Id(registration.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("existsByRegistration_Id is true once a result was recorded")
    void existsByRegistrationId_isTrueAfterRecording() {
        RaceRegistration registration = persistIndividualRegistration();
        persistResult(registration, ResultStatus.DID_NOT_START, null);

        assertTrue(resultRepository.existsByRegistration_Id(registration.getId()));
    }

    @Test
    @DisplayName("existsByRegistration_Id is false for a registration with no result yet")
    void existsByRegistrationId_isFalse_whenNoResultYet() {
        RaceRegistration registration = persistIndividualRegistration();

        assertFalse(resultRepository.existsByRegistration_Id(registration.getId()));
    }

    @Test
    @DisplayName("existsByRace_IdAndFinalPositionAndStatusAndIdNot detects a taken position")
    void existsPositionConflict_detectsTakenPosition() {
        RaceRegistration registration = persistIndividualRegistration();
        persistResult(registration, ResultStatus.FINISHED, 1);

        boolean exists = resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                race.getId(), 1, ResultStatus.FINISHED, UUID.randomUUID());

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByRace_IdAndFinalPositionAndStatusAndIdNot excludes the result's own id")
    void existsPositionConflict_excludesItself() {
        RaceRegistration registration = persistIndividualRegistration();
        RaceResult existing = persistResult(registration, ResultStatus.FINISHED, 1);

        boolean exists = resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                race.getId(), 1, ResultStatus.FINISHED, existing.getId());

        assertFalse(exists);
    }

    @Test
    @DisplayName("findAllByRegistration_CompetitorIsNotNull returns only individual results")
    void findAllIndividualResults_returnsOnlyCompetitorResults() {
        RaceRegistration individualRegistration = persistIndividualRegistration();
        RaceRegistration teamRegistration = persistTeamRegistration();
        persistResult(individualRegistration, ResultStatus.FINISHED, 1);
        persistResult(teamRegistration, ResultStatus.FINISHED, 2);

        List<RaceResult> results = resultRepository.findAllByRegistration_CompetitorIsNotNull();

        assertEquals(1, results.size());
        assertNotNull(results.get(0).getRegistration().getCompetitor());
    }

    @Test
    @DisplayName("findAllByRegistration_TeamIsNotNull returns only team results")
    void findAllTeamResults_returnsOnlyTeamResults() {
        RaceRegistration individualRegistration = persistIndividualRegistration();
        RaceRegistration teamRegistration = persistTeamRegistration();
        persistResult(individualRegistration, ResultStatus.FINISHED, 1);
        persistResult(teamRegistration, ResultStatus.FINISHED, 2);

        List<RaceResult> results = resultRepository.findAllByRegistration_TeamIsNotNull();

        assertEquals(1, results.size());
        assertNotNull(results.get(0).getRegistration().getTeam());
    }
}