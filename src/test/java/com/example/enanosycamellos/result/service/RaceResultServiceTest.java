package com.example.enanosycamellos.result.service;

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
import com.example.enanosycamellos.registration.entity.RaceRegistration;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.registration.repository.IRaceRegistrationRepository;
import com.example.enanosycamellos.result.dto.RaceResultRequest;
import com.example.enanosycamellos.result.dto.RaceResultResponse;
import com.example.enanosycamellos.result.dto.RaceResultUpdateRequest;
import com.example.enanosycamellos.result.entity.RaceResult;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.result.repository.IRaceResultRepository;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.example.enanosycamellos.team.repository.ITeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class RaceResultServiceTest {

    @Mock private IRaceResultRepository resultRepository;
    @Mock private IRaceRegistrationRepository registrationRepository;
    @Mock private IRaceRepository raceRepository;
    @Mock private ICompetitorRepository competitorRepository;
    @Mock private ITeamRepository teamRepository;

    @InjectMocks
    private RaceResultService resultService;

    private UUID raceId;
    private UUID registrationId;
    private UUID competitorId;

    @BeforeEach
    void setUp() {
        raceId = UUID.randomUUID();
        registrationId = UUID.randomUUID();
        competitorId = UUID.randomUUID();
    }

    // =========================== helper builders ============================

    private Race buildRace(RaceStatus status) {
        return Race.builder()
                .id(raceId)
                .name("Test Race")
                .scheduledDateTime(LocalDateTime.now().plusDays(5))
                .startLocation("Start")
                .finishLocation("Finish")
                .distanceMeters(1000)
                .maxParticipants(6)
                .raceType(RaceType.MIXED)
                .status(status)
                .organizer("organizer")
                .registrationDeadline(LocalDateTime.now().plusDays(3))
                .build();
    }

    private Competitor buildCompetitor(UUID id) {
        return Competitor.builder()
                .id(id)
                .name("Byte")
                .nickname("byte-" + id)
                .competitorType(CompetitorType.CAMEL)
                .height(1.9)
                .weight(300.0)
                .origin("Alto de Las Palmas")
                .status(CompetitorStatus.ACTIVE)
                .victories(0)
                .defeats(0)
                .completedRaces(0)
                .build();
    }

    private Team buildTeam(UUID id, Competitor... members) {
        Team team = Team.builder()
                .id(id)
                .name("The Five Exceptions")
                .status(TeamStatus.ACTIVE)
                .coach("coach")
                .maxMembers(6)
                .victories(0)
                .defeats(0)
                .build();
        for (Competitor member : members) {
            team.addMember(member);
        }
        return team;
    }

    private RaceRegistration buildIndividualRegistration(UUID id, Race race, Competitor competitor,
                                                          RegistrationStatus status) {
        return RaceRegistration.builder()
                .id(id).race(race).competitor(competitor).status(status)
                .startingPosition(2).registeredBy("organizer")
                .registrationDate(LocalDateTime.now())
                .build();
    }

    private RaceRegistration buildTeamRegistration(UUID id, Race race, Team team, RegistrationStatus status) {
        return RaceRegistration.builder()
                .id(id).race(race).team(team).status(status)
                .registeredBy("organizer")
                .registrationDate(LocalDateTime.now())
                .build();
    }

    private RaceResultRequest buildRequest(UUID registrationId, ResultStatus status,
                                            Integer finalPosition, Double completionTime) {
        return new RaceResultRequest(registrationId, null, finalPosition, completionTime, null, status, null, "organizer");
    }

    private RaceResult buildResult(UUID id, RaceRegistration registration, ResultStatus status, Integer finalPosition) {
        return RaceResult.builder()
                .id(id).registration(registration).status(status).finalPosition(finalPosition)
                .startPosition(1).completionTime(status == ResultStatus.FINISHED ? 120.0 : null)
                .penaltyTime(0.0).recordedBy("organizer").recordedAt(LocalDateTime.now())
                .build();
    }

    // ============================== record() ==============================

    @Test
    void record_shouldThrowBadRequest_whenCompletionTimeMissingForFinished() {
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.FINISHED, 1, null);

        assertThatThrownBy(() -> resultService.record(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completionTime");

        verify(registrationRepository, never()).findById(any());
    }

    @Test
    void record_shouldThrowBadRequest_whenFinalPositionSetButStatusIsNotFinished() {
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.DISQUALIFIED, 1, null);

        assertThatThrownBy(() -> resultService.record(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("finalPosition");
    }

    @Test
    void record_shouldThrowNotFound_whenRegistrationDoesNotExist() {
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.DID_NOT_START, null, null);
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.record(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void record_shouldThrowConflict_whenRegistrationIsNotApproved() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.PENDING);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.DID_NOT_START, null, null);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> resultService.record(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void record_shouldThrowConflict_whenRaceIsNotInProgress() {
        Race race = buildRace(RaceStatus.OPEN_FOR_REGISTRATION);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.DID_NOT_START, null, null);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> resultService.record(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    void record_shouldThrowConflict_whenRegistrationAlreadyHasResult() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.DID_NOT_START, null, null);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(resultRepository.existsByRegistration_Id(registrationId)).thenReturn(true);

        assertThatThrownBy(() -> resultService.record(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has a result");
    }

    @Test
    void record_shouldThrowConflict_whenFinalPositionIsTaken() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.FINISHED, 1, 120.0);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(resultRepository.existsByRegistration_Id(registrationId)).thenReturn(false);
        when(resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> resultService.record(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already taken");

        verify(resultRepository, never()).save(any());
    }

    @Test
    void record_shouldSucceedAndIncrementVictories_whenIndividualFinishesFirst() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.FINISHED, 1, 120.0);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(resultRepository.existsByRegistration_Id(registrationId)).thenReturn(false);
        when(resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                any(), any(), any(), any())).thenReturn(false);
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(inv -> {
            RaceResult r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RaceResultResponse response = resultService.record(request);

        assertThat(response.finalPosition()).isEqualTo(1);
        assertThat(competitor.getVictories()).isEqualTo(1);
        assertThat(competitor.getDefeats()).isEqualTo(0);
        assertThat(competitor.getCompletedRaces()).isEqualTo(1);
        verify(competitorRepository).save(competitor);
    }

    @Test
    void record_shouldSucceedAndIncrementDefeats_whenIndividualFinishesNotFirst() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.FINISHED, 3, 150.0);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(resultRepository.existsByRegistration_Id(registrationId)).thenReturn(false);
        when(resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                any(), any(), any(), any())).thenReturn(false);
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(inv -> inv.getArgument(0));

        resultService.record(request);

        assertThat(competitor.getVictories()).isEqualTo(0);
        assertThat(competitor.getDefeats()).isEqualTo(1);
        assertThat(competitor.getCompletedRaces()).isEqualTo(1);
    }

    @Test
    void record_shouldNotChangeStats_whenStatusIsNotFinished() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.DID_NOT_FINISH, null, null);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(resultRepository.existsByRegistration_Id(registrationId)).thenReturn(false);
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(inv -> inv.getArgument(0));

        resultService.record(request);

        assertThat(competitor.getVictories()).isEqualTo(0);
        assertThat(competitor.getDefeats()).isEqualTo(0);
        assertThat(competitor.getCompletedRaces()).isEqualTo(0);
        verify(competitorRepository, never()).save(any());
    }

    @Test
    void record_shouldUpdateTeamAndAllMembers_whenTeamFinishesFirst() {
        UUID teamId = UUID.randomUUID();
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor member1 = buildCompetitor(UUID.randomUUID());
        Competitor member2 = buildCompetitor(UUID.randomUUID());
        Team team = buildTeam(teamId, member1, member2);
        RaceRegistration registration =
                buildTeamRegistration(registrationId, race, team, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.FINISHED, 1, 100.0);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(resultRepository.existsByRegistration_Id(registrationId)).thenReturn(false);
        when(resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                any(), any(), any(), any())).thenReturn(false);
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(inv -> inv.getArgument(0));

        resultService.record(request);

        assertThat(team.getVictories()).isEqualTo(1);
        assertThat(member1.getVictories()).isEqualTo(1);
        assertThat(member1.getCompletedRaces()).isEqualTo(1);
        assertThat(member2.getVictories()).isEqualTo(1);
        verify(teamRepository).save(team);
        verify(competitorRepository, times(2)).save(any(Competitor.class));
    }

    @Test
    void record_shouldUseRegistrationStartingPosition_whenNoneProvidedInRequest() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResultRequest request = buildRequest(registrationId, ResultStatus.DID_NOT_START, null, null);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(resultRepository.existsByRegistration_Id(registrationId)).thenReturn(false);

        ArgumentCaptor<RaceResult> captor = ArgumentCaptor.forClass(RaceResult.class);
        when(resultRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        resultService.record(request);

        assertThat(captor.getValue().getStartPosition()).isEqualTo(registration.getStartingPosition());
    }

    // ============================== update() ==============================

    @Test
    void update_shouldThrowNotFound_whenResultDoesNotExist() {
        UUID id = UUID.randomUUID();
        RaceResultUpdateRequest request =
                new RaceResultUpdateRequest(null, null, null, null, ResultStatus.DID_NOT_START, null, "organizer");
        when(resultRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldThrowConflict_whenRaceIsNoLongerInProgress() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(RaceStatus.COMPLETED);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResult existing = buildResult(id, registration, ResultStatus.FINISHED, 2);
        RaceResultUpdateRequest request =
                new RaceResultUpdateRequest(null, 1, 100.0, null, ResultStatus.FINISHED, null, "organizer");

        when(resultRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> resultService.update(id, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_shouldReverseOldStatsAndApplyNew_whenPositionChangesFromLosingToWinning() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        competitor.setDefeats(1);
        competitor.setCompletedRaces(1);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResult existing = buildResult(id, registration, ResultStatus.FINISHED, 3);
        RaceResultUpdateRequest request =
                new RaceResultUpdateRequest(null, 1, 100.0, null, ResultStatus.FINISHED, "corrected", "organizer");

        when(resultRepository.findById(id)).thenReturn(Optional.of(existing));
        when(resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                any(), any(), any(), any())).thenReturn(false);
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(inv -> inv.getArgument(0));

        resultService.update(id, request);

        // -1 defeat (undo), -0... then +1 victory: net result is 0 defeats, 1 victory, completedRaces stays 1.
        assertThat(competitor.getDefeats()).isEqualTo(0);
        assertThat(competitor.getVictories()).isEqualTo(1);
        assertThat(competitor.getCompletedRaces()).isEqualTo(1);
    }

    @Test
    void update_shouldExcludeItselfFromPositionConflictCheck() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResult existing = buildResult(id, registration, ResultStatus.FINISHED, 1);
        RaceResultUpdateRequest request =
                new RaceResultUpdateRequest(null, 1, 105.0, null, ResultStatus.FINISHED, "adjusted time", "organizer");

        when(resultRepository.findById(id)).thenReturn(Optional.of(existing));
        when(resultRepository.existsByRegistration_Race_IdAndFinalPositionAndStatusAndIdNot(
                eq(race.getId()), eq(1), eq(ResultStatus.FINISHED), eq(id))).thenReturn(false);
        when(resultRepository.save(any(RaceResult.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceResultResponse response = resultService.update(id, request);

        assertThat(response.completionTime()).isEqualTo(105.0);
    }

    // ============================== getByRace() / getById() ==============================

    @Test
    void getByRace_shouldReturnList_whenRaceExists() {
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResult result = buildResult(UUID.randomUUID(), registration, ResultStatus.FINISHED, 1);

        when(raceRepository.findById(raceId)).thenReturn(Optional.of(race));
        when(resultRepository.findAllByRegistration_Race_Id(raceId)).thenReturn(List.of(result));

        List<RaceResultResponse> response = resultService.getByRace(raceId);

        assertThat(response).hasSize(1);
    }

    @Test
    void getByRace_shouldThrowNotFound_whenRaceDoesNotExist() {
        when(raceRepository.findById(raceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.getByRace(raceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_shouldReturnResult_whenItExists() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(RaceStatus.IN_PROGRESS);
        Competitor competitor = buildCompetitor(competitorId);
        RaceRegistration registration =
                buildIndividualRegistration(registrationId, race, competitor, RegistrationStatus.APPROVED);
        RaceResult result = buildResult(id, registration, ResultStatus.FINISHED, 1);

        when(resultRepository.findById(id)).thenReturn(Optional.of(result));

        RaceResultResponse response = resultService.getById(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void getById_shouldThrowNotFound_whenResultDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(resultRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}