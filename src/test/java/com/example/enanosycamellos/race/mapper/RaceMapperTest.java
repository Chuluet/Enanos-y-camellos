package com.example.enanosycamellos.race.mapper;

import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the race mapper. Pure tests: no Spring, no mocks — same
 * approach as TeamMapperTest / RegistrationMapperTest.
 */
class RaceMapperTest {

    private RaceRequest buildRequest() {
        return new RaceRequest(
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

    private Race buildRace(RaceStatus status) {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Byte's Revenge 1K")
                .description("Camel vs five dwarfs")
                .scheduledDateTime(LocalDateTime.now().plusDays(5))
                .startLocation("North gate")
                .finishLocation("Trophy stand")
                .distanceMeters(1000)
                .maxParticipants(6)
                .raceType(RaceType.MIXED)
                .status(status)
                .organizer("mr.abandonado")
                .registrationDeadline(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ============================== toEntity ==============================

    @Test
    @DisplayName("toEntity maps every field from the request")
    void toEntity_mapsFields() {
        RaceRequest request = buildRequest();

        Race race = RaceMapper.toEntity(request);

        assertEquals(request.name(), race.getName());
        assertEquals(request.description(), race.getDescription());
        assertEquals(request.scheduledDateTime(), race.getScheduledDateTime());
        assertEquals(request.startLocation(), race.getStartLocation());
        assertEquals(request.finishLocation(), race.getFinishLocation());
        assertEquals(request.distanceMeters(), race.getDistanceMeters());
        assertEquals(request.maxParticipants(), race.getMaxParticipants());
        assertEquals(request.raceType(), race.getRaceType());
        assertEquals(request.organizer(), race.getOrganizer());
        assertEquals(request.registrationDeadline(), race.getRegistrationDeadline());
    }

    @Test
    @DisplayName("toEntity always starts a race in DRAFT status")
    void toEntity_alwaysStartsInDraft() {
        Race race = RaceMapper.toEntity(buildRequest());

        assertEquals(RaceStatus.DRAFT, race.getStatus());
    }

    @Test
    @DisplayName("toEntity with null returns null")
    void toEntity_withNull_returnsNull() {
        assertNull(RaceMapper.toEntity(null));
    }

    // ============================== toResponse ==============================

    @Test
    @DisplayName("toResponse maps every field, including timestamps")
    void toResponse_mapsFields() {
        Race race = buildRace(RaceStatus.OPEN_FOR_REGISTRATION);

        RaceResponse response = RaceMapper.toResponse(race);

        assertEquals(race.getId(), response.id());
        assertEquals(race.getName(), response.name());
        assertEquals(race.getStatus(), response.status());
        assertEquals(race.getCreatedAt(), response.createdAt());
        assertEquals(race.getUpdatedAt(), response.updatedAt());
    }

    @Test
    @DisplayName("toResponse with null returns null")
    void toResponse_withNull_returnsNull() {
        assertNull(RaceMapper.toResponse(null));
    }

    // ============================== toSummary ==============================

    @Test
    @DisplayName("toSummary maps only the short fields")
    void toSummary_mapsShortFields() {
        Race race = buildRace(RaceStatus.DRAFT);

        RaceSummaryResponse summary = RaceMapper.toSummary(race);

        assertEquals(race.getId(), summary.id());
        assertEquals(race.getName(), summary.name());
        assertEquals(race.getRaceType(), summary.raceType());
        assertEquals(race.getStatus(), summary.status());
        assertEquals(race.getScheduledDateTime(), summary.scheduledDateTime());
    }

    @Test
    @DisplayName("toSummary with null returns null")
    void toSummary_withNull_returnsNull() {
        assertNull(RaceMapper.toSummary(null));
    }

    // ============================== toResponseList ==============================

    @Test
    @DisplayName("toResponseList maps every element in order")
    void toResponseList_mapsAllElements() {
        Race race1 = buildRace(RaceStatus.DRAFT);
        Race race2 = buildRace(RaceStatus.OPEN_FOR_REGISTRATION);

        List<RaceResponse> result = RaceMapper.toResponseList(List.of(race1, race2));

        assertEquals(2, result.size());
        assertEquals(race1.getId(), result.get(0).id());
        assertEquals(race2.getId(), result.get(1).id());
    }

    @Test
    @DisplayName("toResponseList with null returns an empty list")
    void toResponseList_withNull_returnsEmptyList() {
        assertTrue(RaceMapper.toResponseList(null).isEmpty());
    }
}