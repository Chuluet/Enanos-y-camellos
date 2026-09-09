package com.example.enanosycamellos.race.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.dto.RaceUpdateRequest;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.repository.IRaceRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaceServiceTest {

    @Mock
    private IRaceRepository raceRepository;

    @InjectMocks
    private RaceService raceService;

    private RaceRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RaceRequest(
                "Byte's Revenge 1K",
                "Camel vs five dwarfs",
                LocalDateTime.now().plusDays(5),
                "North gate",
                "Trophy stand",
                1000,
                6,
                RaceType.MIXED,
                "mr.abandonado",
                LocalDateTime.now().plusDays(3)
        );
    }

    @Test
    void create_shouldSaveRace_whenDataIsValid() {
        when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> {
            Race race = invocation.getArgument(0);
            race.setId(UUID.randomUUID());
            return race;
        });

        RaceResponse response = raceService.create(validRequest);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Byte's Revenge 1K");
        assertThat(response.status()).isEqualTo(RaceStatus.DRAFT);
        verify(raceRepository).save(any(Race.class));
    }

    @Test
    void create_shouldThrowBadRequest_whenDeadlineIsAfterScheduledDateTime() {
        RaceRequest invalidRequest = new RaceRequest(
                "Bad Race",
                "desc",
                LocalDateTime.now().plusDays(3),
                "start",
                "finish",
                1000,
                6,
                RaceType.INDIVIDUAL,
                "organizer",
                LocalDateTime.now().plusDays(5) // deadline AFTER the race starts: invalid
        );

        assertThatThrownBy(() -> raceService.create(invalidRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("registrationDeadline");

        verify(raceRepository, never()).save(any());
    }
        // ============================== update() ==============================

    @Test
    void update_shouldThrowBadRequest_whenRequestIsEmpty() {
        RaceUpdateRequest emptyRequest = new RaceUpdateRequest(
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> raceService.update(UUID.randomUUID(), emptyRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no fields to update");

        verify(raceRepository, never()).findById(any());
    }

    @Test
    void update_shouldThrowNotFound_whenRaceDoesNotExist() {
        UUID id = UUID.randomUUID();
        RaceUpdateRequest request = new RaceUpdateRequest(
                "New name", null, null, null, null, null, null, null);

        when(raceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> raceService.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldThrowConflict_whenRaceIsCompleted() {
        UUID id = UUID.randomUUID();
        Race completedRace = buildRace(id, RaceStatus.COMPLETED);
        RaceUpdateRequest request = new RaceUpdateRequest(
                "New name", null, null, null, null, null, null, null);

        when(raceRepository.findById(id)).thenReturn(Optional.of(completedRace));

        assertThatThrownBy(() -> raceService.update(id, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("completed race cannot be edited");

        verify(raceRepository, never()).save(any());
    }

    @Test
    void update_shouldChangeOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        Race existingRace = buildRace(id, RaceStatus.DRAFT);
        String originalDescription = existingRace.getDescription();

        RaceUpdateRequest request = new RaceUpdateRequest(
                "Updated name", null, null, null, null, null, null, null);

        when(raceRepository.findById(id)).thenReturn(Optional.of(existingRace));
        when(raceRepository.save(any(Race.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceResponse response = raceService.update(id, request);

        assertThat(response.name()).isEqualTo("Updated name");
        assertThat(response.description()).isEqualTo(originalDescription); // no se tocó
    }

    // =========================== helper builder ============================

    private Race buildRace(UUID id, RaceStatus status) {
        return Race.builder()
                .id(id)
                .name("Test Race")
                .description("Test description")
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
        // ============================ changeStatus() ===========================

    @Test
    void changeStatus_shouldThrowNotFound_whenRaceDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(raceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> raceService.changeStatus(id, RaceStatus.OPEN_FOR_REGISTRATION))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatus_shouldThrowConflict_whenAlreadyInTargetStatus() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.DRAFT);
        when(raceRepository.findById(id)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> raceService.changeStatus(id, RaceStatus.DRAFT))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in status");
    }

    @Test
    void changeStatus_shouldAllowValidTransition_draftToOpenForRegistration() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.DRAFT);

        when(raceRepository.findById(id)).thenReturn(Optional.of(race));
        when(raceRepository.save(any(Race.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceResponse response = raceService.changeStatus(id, RaceStatus.OPEN_FOR_REGISTRATION);

        assertThat(response.status()).isEqualTo(RaceStatus.OPEN_FOR_REGISTRATION);
    }

    @Test
    void changeStatus_shouldThrowConflict_whenTransitionSkipsStates() {
        // DRAFT no puede saltar directo a IN_PROGRESS, tiene que pasar
        // por OPEN_FOR_REGISTRATION y CLOSED_FOR_REGISTRATION primero.
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.DRAFT);
        when(raceRepository.findById(id)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> raceService.changeStatus(id, RaceStatus.IN_PROGRESS))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot move race from DRAFT to IN_PROGRESS");

        verify(raceRepository, never()).save(any());
    }

    @Test
    void changeStatus_shouldThrowConflict_whenCurrentStatusIsCompleted() {
        // COMPLETED es terminal: ni siquiera puede pasar a CANCELLED.
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.COMPLETED);
        when(raceRepository.findById(id)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> raceService.changeStatus(id, RaceStatus.CANCELLED))
                .isInstanceOf(ConflictException.class);

        verify(raceRepository, never()).save(any());
    }

    @Test
    void changeStatus_shouldAllowCancelFromAnyNonTerminalState() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.OPEN_FOR_REGISTRATION);

        when(raceRepository.findById(id)).thenReturn(Optional.of(race));
        when(raceRepository.save(any(Race.class))).thenAnswer(inv -> inv.getArgument(0));

        RaceResponse response = raceService.changeStatus(id, RaceStatus.CANCELLED);

        assertThat(response.status()).isEqualTo(RaceStatus.CANCELLED);
    }

    // =============================== cancel() ===============================

    @Test
    void cancel_shouldSetStatusToCancelled() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.DRAFT);

        when(raceRepository.findById(id)).thenReturn(Optional.of(race));
        when(raceRepository.save(any(Race.class))).thenAnswer(inv -> inv.getArgument(0));

        raceService.cancel(id);

        assertThat(race.getStatus()).isEqualTo(RaceStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowConflict_whenRaceIsAlreadyCompleted() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.COMPLETED);
        when(raceRepository.findById(id)).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> raceService.cancel(id))
                .isInstanceOf(ConflictException.class);
    }
        // ================================ getById() =============================

    @Test
    void getById_shouldReturnRace_whenItExists() {
        UUID id = UUID.randomUUID();
        Race race = buildRace(id, RaceStatus.DRAFT);
        when(raceRepository.findById(id)).thenReturn(Optional.of(race));

        RaceResponse response = raceService.getById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo(race.getName());
    }

    @Test
    void getById_shouldThrowNotFound_whenRaceDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(raceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> raceService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ================================ getRaces() =============================

    @Test
    void getRaces_shouldReturnAll_whenNoFiltersAreGiven() {
        Race race1 = buildRace(UUID.randomUUID(), RaceStatus.DRAFT);
        Race race2 = buildRace(UUID.randomUUID(), RaceStatus.OPEN_FOR_REGISTRATION);
        when(raceRepository.findAllByOrderByScheduledDateTimeAsc())
                .thenReturn(List.of(race1, race2));

        List<RaceResponse> result = raceService.getRaces(null, null);

        assertThat(result).hasSize(2);
        verify(raceRepository).findAllByOrderByScheduledDateTimeAsc();
    }

    @Test
    void getRaces_shouldFilterByStatusOnly_whenOnlyStatusIsGiven() {
        Race race = buildRace(UUID.randomUUID(), RaceStatus.OPEN_FOR_REGISTRATION);
        when(raceRepository.findAllByStatusOrderByScheduledDateTimeAsc(RaceStatus.OPEN_FOR_REGISTRATION))
                .thenReturn(List.of(race));

        List<RaceResponse> result = raceService.getRaces(RaceStatus.OPEN_FOR_REGISTRATION, null);

        assertThat(result).hasSize(1);
        verify(raceRepository).findAllByStatusOrderByScheduledDateTimeAsc(RaceStatus.OPEN_FOR_REGISTRATION);
        verify(raceRepository, never()).findAllByOrderByScheduledDateTimeAsc();
    }

    @Test
    void getRaces_shouldFilterByStatusAndType_whenBothAreGiven() {
        Race race = buildRace(UUID.randomUUID(), RaceStatus.DRAFT);
        when(raceRepository.findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc(
                RaceStatus.DRAFT, RaceType.MIXED))
                .thenReturn(List.of(race));

        List<RaceResponse> result = raceService.getRaces(RaceStatus.DRAFT, RaceType.MIXED);

        assertThat(result).hasSize(1);
        verify(raceRepository).findAllByStatusAndRaceTypeOrderByScheduledDateTimeAsc(
                RaceStatus.DRAFT, RaceType.MIXED);
    }
}