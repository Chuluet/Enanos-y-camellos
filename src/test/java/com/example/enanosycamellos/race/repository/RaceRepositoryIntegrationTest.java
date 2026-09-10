package com.example.enanosycamellos.race.repository;

import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RaceRepositoryIntegrationTest {

    @Autowired
    private IRaceRepository raceRepository;

    @Autowired
    private TestEntityManager em;

    private Race persistRace(String name, RaceStatus status, RaceType type, LocalDateTime scheduledDateTime) {
        Race race = Race.builder()
                .name(name)
                .description("desc")
                .scheduledDateTime(scheduledDateTime)
                .startLocation("Start")
                .finishLocation("Finish")
                .distanceMeters(1000)
                .maxParticipants(6)
                .raceType(type)
                .status(status)
                .organizer("organizer")
                .registrationDeadline(scheduledDateTime.minusDays(1))
                .build();
        return em.persistAndFlush(race);
    }

    @BeforeEach
    void setUp() {
        persistRace("Race A", RaceStatus.DRAFT, RaceType.INDIVIDUAL, LocalDateTime.now().plusDays(3));
        persistRace("Race B", RaceStatus.OPEN_FOR_REGISTRATION, RaceType.TEAM, LocalDateTime.now().plusDays(1));
        persistRace("Race C", RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED, LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("findAllByOrderByScheduledDateTimeAsc returns every race, soonest first")
    void findAllOrderedByScheduledDateTime_returnsAllRacesInOrder() {
        List<Race> result = raceRepository.findAllByOrderByScheduledDateTimeAsc();

        assertEquals(3, result.size());
        assertEquals("Race B", result.get(0).getName()); // +1 day: soonest
        assertEquals("Race C", result.get(1).getName()); // +2 days
        assertEquals("Race A", result.get(2).getName()); // +3 days
    }

    @Test
    @DisplayName("findAllByStatusOrderByScheduledDateTimeAsc filters by status only")
    void findAllByStatus_returnsOnlyMatchingStatus() {
        List<Race> result =
                raceRepository.findAllByStatusOrderByScheduledDateTimeAsc(RaceStatus.OPEN_FOR_REGISTRATION);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getStatus() == RaceStatus.OPEN_FOR_REGISTRATION));
    }

    @Test
    @DisplayName("findAllByRaceTypeOrderByScheduledDateTimeAsc filters by type only")
    void findAllByType_returnsOnlyMatchingType() {
        List<Race> result = raceRepository.findAllByRaceTypeOrderByScheduledDateTimeAsc(RaceType.MIXED);

        assertEquals(1, result.size());
        assertEquals("Race C", result.get(0).getName());
    }

    @Test
    @DisplayName("findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc combines both filters")
    void findAllByStatusAndType_combinesBothFilters() {
        List<Race> result = raceRepository.findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc(
                RaceStatus.OPEN_FOR_REGISTRATION, RaceType.TEAM);

        assertEquals(1, result.size());
        assertEquals("Race B", result.get(0).getName());
    }

    @Test
    @DisplayName("findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc returns empty when nothing matches")
    void findAllByStatusAndType_returnsEmpty_whenNoMatch() {
        List<Race> result = raceRepository.findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc(
                RaceStatus.COMPLETED, RaceType.INDIVIDUAL);

        assertTrue(result.isEmpty());
    }
}