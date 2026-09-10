package com.example.enanosycamellos.registration.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.repository.IRaceRepository;
import com.example.enanosycamellos.registration.dto.RaceRegistrationRequest;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.registration.repository.IRaceRegistrationRepository;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.example.enanosycamellos.team.repository.ITeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private IRaceRegistrationRepository registrationRepository;
    @Mock private IRaceRepository raceRepository;
    @Mock private ICompetitorRepository competitorRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private com.example.enanosycamellos.auditlog.service.AuditLogService auditLogService;

    @InjectMocks
    private RegistrationService registrationService;

    private UUID raceId;
    private UUID competitorId;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        raceId = UUID.randomUUID();
        competitorId = UUID.randomUUID();
        teamId = UUID.randomUUID();
    }

    // =========================== helper builders ============================

    private Race buildRace(UUID id, RaceStatus status, RaceType type) {
        return Race.builder()
                .id(id)
                .name("Test Race")
                .scheduledDateTime(LocalDateTime.now().plusDays(5))
                .startLocation("Start")
                .finishLocation("Finish")
                .distanceMeters(1000)
                .maxParticipants(6)
                .raceType(type)
                .status(status)
                .organizer("organizer")
                .registrationDeadline(LocalDateTime.now().plusDays(3))
                .build();
    }

    private Competitor buildCompetitor(UUID id, CompetitorStatus status) {
        return Competitor.builder()
                .id(id)
                .name("Byte")
                .nickname("byte-" + id)
                .competitorType(CompetitorType.CAMEL)
                .height(1.9)
                .weight(300.0)
                .origin("Alto de Las Palmas")
                .status(status)
                .build();
    }

    private Team buildTeam(UUID id, TeamStatus status, Competitor... members) {
        Team team = Team.builder()
                .id(id)
                .name("The Five Exceptions")
                .status(status)
                .coach("coach")
                .maxMembers(6)
                .build();
        for (Competitor member : members) {
            team.addMember(member);
        }
        return team;
    }

    private RaceRegistrationRequest individualRequest(UUID competitorId, Integer startingPosition) {
        return new RaceRegistrationRequest(competitorId, null, startingPosition, "organizer");
    }

    private RaceRegistrationRequest teamRequest(UUID teamId, Integer startingPosition) {
        return new RaceRegistrationRequest(null, teamId, startingPosition, "organizer");
    }

    private RaceRegistration buildRegistration(UUID id, Race race, Competitor competitor, Team team,
                                                RegistrationStatus status) {
        return RaceRegistration.builder()
                .id(id)
                .race(race)
                .competitor(competitor)
                .team(team)
                .status(status)
                .registrationDate(LocalDateTime.now())
                .registeredBy("organizer")
                .build();
    }

    // ======================= register(): validación general =======================

    @Test
    void register_shouldThrowBadRequest_whenBothCompetitorAndTeamAreSent() {
        RaceRegistrationRequest request = new RaceRegistrationRequest(
                UUID.randomUUID(), UUID.randomUUID(), null, "organizer");

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exactly one");

        verify(raceRepository, never()).findById(any());
    }

    @Test
    void register_shouldThrowBadRequest_whenNeitherCompetitorNorTeamIsSent() {
        RaceRegistrationRequest request = new RaceRegistrationRequest(null, null, null, "organizer");

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void register_shouldThrowNotFound_whenRaceDoesNotExist() {
        RaceRegistrationRequest request = individualRequest(competitorId, null);
        when(raceRepository.findById(raceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void register_shouldThrowConflict_whenRaceIsNotOpenForRegistration() {
        Race race = buildRace(raceId, RaceStatus.DRAFT, RaceType.MIXED);
        RaceRegistrationRequest request = individualRequest(competitorId, null);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not open for registration");
    }

    @Test
    void register_shouldThrowConflict_whenDeadlineHasPassed() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        race.setRegistrationDeadline(LocalDateTime.now().minusHours(1));
        RaceRegistrationRequest request = individualRequest(competitorId, null);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    void register_shouldThrowBadRequest_whenIndividualRegistersInTeamOnlyRace() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.TEAM);
        RaceRegistrationRequest request = individualRequest(competitorId, null);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only accepts teams");
    }

    @Test
    void register_shouldThrowBadRequest_whenTeamRegistersInIndividualOnlyRace() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.INDIVIDUAL);
        RaceRegistrationRequest request = teamRequest(teamId, null);
        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only accepts individual");
    }

    // ======================= register(): camino individual =======================

    @Test
    void register_shouldSucceed_whenIndividualCompetitorIsValid() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistrationRequest request = individualRequest(competitorId, 1);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(competitor));
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(registrationRepository.findAllByRace_Id(raceId)).thenReturn(List.of());
        when(registrationRepository.existsByRace_IdAndStartingPositionAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(registrationRepository.save(any(RaceRegistration.class))).thenAnswer(inv -> {
            RaceRegistration r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RaceRegistrationResponse response = registrationService.register(raceId, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(response.competitor().id()).isEqualTo(competitorId);
        assertThat(response.team()).isNull();
    }

    @Test
    void register_shouldThrowNotFound_whenCompetitorDoesNotExist() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        RaceRegistrationRequest request = individualRequest(competitorId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void register_shouldThrowConflict_whenCompetitorIsNotActive() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.SUSPENDED);
        RaceRegistrationRequest request = individualRequest(competitorId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(competitor));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void register_shouldThrowConflict_whenCompetitorAlreadyRegisteredInRace() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistrationRequest request = individualRequest(competitorId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(competitor));
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_shouldThrowConflict_whenCompetitorAlreadyRegisteredAsPartOfTeam() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        Team otherTeam = buildTeam(UUID.randomUUID(), TeamStatus.ACTIVE, competitor);
        RaceRegistration existingTeamRegistration =
                buildRegistration(UUID.randomUUID(), race, null, otherTeam, RegistrationStatus.APPROVED);
        RaceRegistrationRequest request = individualRequest(competitorId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(competitor));
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(registrationRepository.findAllByRace_Id(raceId)).thenReturn(List.of(existingTeamRegistration));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("part of a team");
    }

    // ======================= register(): camino de equipo =======================

    @Test
    void register_shouldSucceed_whenTeamIsValid() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor member = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE);
        Team team = buildTeam(teamId, TeamStatus.ACTIVE, member);
        RaceRegistrationRequest request = teamRequest(teamId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.of(team));
        when(registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(registrationRepository.existsByRace_IdAndCompetitor_IdInAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(registrationRepository.save(any(RaceRegistration.class))).thenAnswer(inv -> {
            RaceRegistration r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RaceRegistrationResponse response = registrationService.register(raceId, request);

        assertThat(response.team().id()).isEqualTo(teamId);
        assertThat(response.competitor()).isNull();
    }

    @Test
    void register_shouldThrowNotFound_whenTeamDoesNotExist() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        RaceRegistrationRequest request = teamRequest(teamId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void register_shouldThrowConflict_whenTeamIsNotActive() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor member = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE);
        Team team = buildTeam(teamId, TeamStatus.SUSPENDED, member);
        RaceRegistrationRequest request = teamRequest(teamId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void register_shouldThrowConflict_whenTeamHasNoMembers() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Team team = buildTeam(teamId, TeamStatus.ACTIVE);
        RaceRegistrationRequest request = teamRequest(teamId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("at least one member");
    }

    @Test
    void register_shouldThrowConflict_whenNotAllTeamMembersAreActive() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor activeMember = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE);
        Competitor injuredMember = buildCompetitor(UUID.randomUUID(), CompetitorStatus.INJURED);
        Team team = buildTeam(teamId, TeamStatus.ACTIVE, activeMember, injuredMember);
        RaceRegistrationRequest request = teamRequest(teamId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("must be ACTIVE");
    }

    @Test
    void register_shouldThrowConflict_whenTeamAlreadyRegistered() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor member = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE);
        Team team = buildTeam(teamId, TeamStatus.ACTIVE, member);
        RaceRegistrationRequest request = teamRequest(teamId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.of(team));
        when(registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Team is already registered");
    }

    @Test
    void register_shouldThrowConflict_whenTeamMemberAlreadyRegisteredIndividually() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor member = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE);
        Team team = buildTeam(teamId, TeamStatus.ACTIVE, member);
        RaceRegistrationRequest request = teamRequest(teamId, null);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.of(team));
        when(registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(registrationRepository.existsByRace_IdAndCompetitor_IdInAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("registered individually");
    }

    // ======================= register(): posición de salida =======================

    @Test
    void register_shouldThrowConflict_whenStartingPositionIsTaken() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistrationRequest request = individualRequest(competitorId, 1);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(competitor));
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(false);
        when(registrationRepository.findAllByRace_Id(raceId)).thenReturn(List.of());
        when(registrationRepository.existsByRace_IdAndStartingPositionAndStatusIn(any(), any(), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(raceId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already taken");

        verify(registrationRepository, never()).save(any());
    }

    // ============================== approve() ==============================

    @Test
    void approve_shouldSucceed_whenRegistrationIsPending() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(id, race, competitor, null, RegistrationStatus.PENDING);

        when(registrationRepository.findById(id)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(RaceRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceRegistrationResponse response = registrationService.approve(id);

        assertThat(response.status()).isEqualTo(RegistrationStatus.APPROVED);
    }

    @Test
    void approve_shouldThrowConflict_whenRegistrationIsNotPending() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(id, race, competitor, null, RegistrationStatus.APPROVED);

        when(registrationRepository.findById(id)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.approve(id))
                .isInstanceOf(ConflictException.class);

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void approve_shouldThrowNotFound_whenRegistrationDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(registrationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.approve(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============================== reject() ==============================

    @Test
    void reject_shouldSetStatusAndValidationNotes() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(id, race, competitor, null, RegistrationStatus.PENDING);

        when(registrationRepository.findById(id)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(RaceRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceRegistrationResponse response = registrationService.reject(id, "Competitor is SUSPENDED");

        assertThat(response.status()).isEqualTo(RegistrationStatus.REJECTED);
        assertThat(response.validationNotes()).isEqualTo("Competitor is SUSPENDED");
    }

    @Test
    void reject_shouldThrowConflict_whenRegistrationIsNotPending() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(id, race, competitor, null, RegistrationStatus.CANCELLED);

        when(registrationRepository.findById(id)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.reject(id, "too late"))
                .isInstanceOf(ConflictException.class);
    }

    // ============================== cancel() ==============================

    @Test
    void cancel_shouldSetStatusToCancelled() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(id, race, competitor, null, RegistrationStatus.PENDING);

        when(registrationRepository.findById(id)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(RaceRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        registrationService.cancel(id);

        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowConflict_whenAlreadyRejected() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(id, race, competitor, null, RegistrationStatus.REJECTED);

        when(registrationRepository.findById(id)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.cancel(id))
                .isInstanceOf(ConflictException.class);
    }

    // ============================== getByRace() / getById() ==============================

    @Test
    void getByRace_shouldReturnList_whenRaceExists() {
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(UUID.randomUUID(), race, competitor, null, RegistrationStatus.PENDING);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(registrationRepository.findAllByRace_Id(raceId)).thenReturn(List.of(registration));

        List<RaceRegistrationResponse> result = registrationService.getByRace(raceId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getByRace_shouldThrowNotFound_whenRaceDoesNotExist() {
        when(raceRepository.findById(raceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.getByRace(raceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_shouldReturnRegistration_whenItExists() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(raceId, RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
        Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE);
        RaceRegistration registration =
                buildRegistration(id, race, competitor, null, RegistrationStatus.PENDING);

        when(registrationRepository.findById(id)).thenReturn(Optional.of(registration));

        RaceRegistrationResponse response = registrationService.getById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void getById_shouldThrowNotFound_whenRegistrationDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(registrationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}