package com.example.enanosycamellos.common.seed;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.service.RaceService;
import com.example.enanosycamellos.registration.dto.RaceRegistrationRequest;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.service.RegistrationService;
import com.example.enanosycamellos.result.dto.RaceResultRequest;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.result.service.RaceResultService;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.example.enanosycamellos.team.repository.ITeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Populates the database with the minimum initial data required by the
 * assignment (section 8), but ONLY on a fresh database — it checks
 * {@code competitorRepository.count() == 0} first, so it never duplicates
 * data on every app restart.
 *
 * <p>Deliberately goes through the real services (RaceService,
 * RegistrationService, RaceResultService) instead of saving entities
 * directly — this exercises the actual state machine, business rules and
 * statistics logic on every boot, which doubles as a smoke test: if any of
 * that logic were broken, the app would fail to start instead of silently
 * shipping broken behavior.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ICompetitorRepository competitorRepository;
    private final ITeamRepository teamRepository;
    private final RaceService raceService;
    private final RegistrationService registrationService;
    private final RaceResultService raceResultService;

    private static final String SEED_USER = "system-seed";

    @Override
    public void run(String... args) {
        if (competitorRepository.count() > 0) {
            log.info("Database already has data, skipping seed.");
            return;
        }

        log.info("Empty database detected, seeding minimum initial data...");

        // ---- Competitors: 5 dwarfs, 2 camels, 2 medium (Module 2) ----
        Competitor nullPointer = seedCompetitor("Null Pointer", "null-pointer", CompetitorType.DWARF, 1.05, 58.0);
        Competitor stackOverflow = seedCompetitor("Stack Overflow", "stack-overflow", CompetitorType.DWARF, 1.1, 61.0);
        Competitor littleLambda = seedCompetitor("Little Lambda", "little-lambda", CompetitorType.DWARF, 0.98, 52.0);
        Competitor captainCache = seedCompetitor("Captain Cache", "captain-cache", CompetitorType.DWARF, 1.12, 65.0);
        Competitor tinyDocker = seedCompetitor("Tiny Docker", "tiny-docker", CompetitorType.DWARF, 1.02, 55.0);

        Competitor byte_ = seedCompetitor("Byte", "byte", CompetitorType.CAMEL, 2.1, 420.0);
        Competitor kernel = seedCompetitor("Kernel", "kernel", CompetitorType.CAMEL, 2.05, 400.0);

        seedCompetitor("Sparky", "sparky", CompetitorType.MEDIUM, 1.55, 150.0);
        seedCompetitor("Nibble", "nibble", CompetitorType.MEDIUM, 1.48, 140.0);

        // ---- Teams (Module 3) ----
        Team fiveExceptions = seedTeam("The Five Exceptions", "coach-turing", 6,
                nullPointer, stackOverflow, littleLambda, captainCache, tinyDocker);

        // ---- Races (Module 4): three in different statuses ----
        seedDraftRace();
        seedOpenRace();
        seedCompletedRaceWithResults(byte_, fiveExceptions);

        log.info("Seed data created successfully.");
    }

    private Competitor seedCompetitor(String name, String nickname, CompetitorType type,
                                       double height, double weight) {
        Competitor competitor = Competitor.builder()
                .name(name)
                .nickname(nickname)
                .competitorType(type)
                .age(3)
                .height(height)
                .weight(weight)
                .origin("Alto de Las Palmas")
                .status(CompetitorStatus.ACTIVE)
                .build();
        return competitorRepository.save(competitor);
    }

    private Team seedTeam(String name, String coach, int maxMembers, Competitor... members) {
        Team team = Team.builder()
                .name(name)
                .description("Seed team")
                .coach(coach)
                .status(TeamStatus.ACTIVE)
                .maxMembers(maxMembers)
                .build();
        Team saved = teamRepository.save(team);
        for (Competitor member : members) {
            saved.addMember(member);
            competitorRepository.save(member);
        }
        return teamRepository.save(saved);
    }

    /** Race 1: stays in DRAFT — never opened. */
    private void seedDraftRace() {
        raceService.create(new RaceRequest(
                "Byte's Qualifier", "A quiet warm-up lap, not opened yet",
                LocalDateTime.now().plusDays(20), "North gate", "Trophy stand",
                800, 4, RaceType.INDIVIDUAL, "organizer",
                LocalDateTime.now().plusDays(15)));
    }

    /** Race 2: open for registration, nobody signed up yet. */
    private void seedOpenRace() {
        RaceResponse race = raceService.create(new RaceRequest(
                "Dwarf Dash Sprint", "Open for registration",
                LocalDateTime.now().plusDays(10), "South gate", "Trophy stand",
                500, 6, RaceType.TEAM, "organizer",
                LocalDateTime.now().plusDays(5)));
        raceService.changeStatus(race.id(), RaceStatus.OPEN_FOR_REGISTRATION);
    }

    /**
     * Race 3: walked all the way through the real state machine to
     * COMPLETED, with an individual (Byte) and a team (The Five Exceptions)
     * registered, approved, and both with a recorded FINISHED result —
     * satisfies "at least one completed race with results" for real, not
     * just by directly writing rows.
     */
    private void seedCompletedRaceWithResults(Competitor individual, Team team) {
        RaceResponse race = raceService.create(new RaceRequest(
                "The Great EIA 1K", "Camel vs five dwarfs, mixed rules",
                LocalDateTime.now().plusMinutes(5), "Alto de Las Palmas - North gate",
                "Alto de Las Palmas - Trophy stand", 1000, 6, RaceType.MIXED,
                "organizer", LocalDateTime.now().plusMinutes(1)));
        UUID raceId = race.id();

        raceService.changeStatus(raceId, RaceStatus.OPEN_FOR_REGISTRATION);

        RaceRegistrationResponse individualRegistration = registrationService.register(
                raceId, new RaceRegistrationRequest(individual.getId(), null, 1, SEED_USER));
        RaceRegistrationResponse teamRegistration = registrationService.register(
                raceId, new RaceRegistrationRequest(null, team.getId(), 2, SEED_USER));

        registrationService.approve(individualRegistration.id());
        registrationService.approve(teamRegistration.id());

        raceService.changeStatus(raceId, RaceStatus.CLOSED_FOR_REGISTRATION);
        raceService.changeStatus(raceId, RaceStatus.IN_PROGRESS);

        raceResultService.record(new RaceResultRequest(
                individualRegistration.id(), 1, 1, 118.4, 0.0,
                ResultStatus.FINISHED, "Byte crosses first, as always", SEED_USER));
        raceResultService.record(new RaceResultRequest(
                teamRegistration.id(), 2, 2, 130.0, 0.0,
                ResultStatus.FINISHED, "Tiny Docker insists it worked on his machine", SEED_USER));

        raceService.changeStatus(raceId, RaceStatus.COMPLETED);
    }
}