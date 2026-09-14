package com.example.enanosycamellos.competitor.mapper;

import com.example.enanosycamellos.competitor.dto.CompetitorRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorResponse;
import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the competitor mapper. Pure tests: no Spring, no mocks —
 * same approach as {@code TeamMapperTest}.
 */
class CompetitorMapperTest {

    // ============================== toEntity ==============================

    @Test
    @DisplayName("toEntity maps every field, including a provided status")
    void toEntity_mapsFields() {
        CompetitorRequest request = new CompetitorRequest(
                "John D", "JD", CompetitorType.DWARF,
                LocalDate.of(1989, 6, 15), null,
                120.0, 50.0, "USA", CompetitorStatus.ACTIVE);

        Competitor competitor = CompetitorMapper.toEntity(request);

        assertNotNull(competitor);
        assertEquals("John D", competitor.getName());
        assertEquals("JD", competitor.getNickname());
        assertEquals(CompetitorType.DWARF, competitor.getCompetitorType());
        assertEquals(LocalDate.of(1989, 6, 15), competitor.getBirthDate());
        assertNull(competitor.getAge());
        assertEquals(120.0, competitor.getHeight());
        assertEquals(50.0, competitor.getWeight());
        assertEquals("USA", competitor.getOrigin());
        assertEquals(CompetitorStatus.ACTIVE, competitor.getStatus());
    }

    @Test
    @DisplayName("toEntity with null returns null")
    void toEntity_withNull_returnsNull() {
        assertNull(CompetitorMapper.toEntity(null));
    }

    // ============================== toResponse ==============================

    @Test
    @DisplayName("toResponse maps all fields, including the team as a summary")
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        Team team = Team.builder()
                .id(UUID.randomUUID()).name("The Five Exceptions").status(TeamStatus.ACTIVE)
                .build();

        Competitor competitor = Competitor.builder()
                .id(id).name("John D").nickname("JD").competitorType(CompetitorType.CAMEL)
                .birthDate(LocalDate.of(1989, 6, 15)).height(180.0).weight(300.0)
                .origin("Egypt").status(CompetitorStatus.ACTIVE)
                .registrationDate(LocalDate.of(2024, 1, 10))
                .victories(4).defeats(1).completedRaces(5)
                .team(team)
                .build();

        CompetitorResponse response = CompetitorMapper.toResponse(competitor);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals("John D", response.name());
        assertEquals("JD", response.nickname());
        assertEquals(CompetitorType.CAMEL, response.competitorType());
        assertEquals(CompetitorStatus.ACTIVE, response.status());
        assertEquals(4, response.victories());
        assertEquals(1, response.defeats());
        assertEquals(5, response.completedRaces());
        assertNotNull(response.team());
        assertEquals("The Five Exceptions", response.team().name());
    }

    @Test
    @DisplayName("toResponse with a teamless competitor leaves team null")
    void toResponse_withoutTeam_leavesTeamNull() {
        Competitor competitor = Competitor.builder()
                .id(UUID.randomUUID()).name("Solo").nickname("SOLO")
                .competitorType(CompetitorType.OTHER).height(100.0).weight(40.0)
                .status(CompetitorStatus.ACTIVE)
                .build();

        CompetitorResponse response = CompetitorMapper.toResponse(competitor);

        assertNull(response.team());
    }

    @Test
    @DisplayName("toResponse with null returns null")
    void toResponse_withNull_returnsNull() {
        assertNull(CompetitorMapper.toResponse(null));
    }

    // ============================== toSummary ==============================

    @Test
    @DisplayName("toSummary maps id, name, nickname, type and status")
    void toSummary_mapsFields() {
        UUID id = UUID.randomUUID();
        Competitor competitor = Competitor.builder()
                .id(id).name("John D").nickname("JD")
                .competitorType(CompetitorType.MEDIUM).status(CompetitorStatus.INJURED)
                .build();

        CompetitorSummaryResponse summary = CompetitorMapper.toSummary(competitor);

        assertNotNull(summary);
        assertEquals(id, summary.id());
        assertEquals("John D", summary.name());
        assertEquals("JD", summary.nickname());
        assertEquals(CompetitorType.MEDIUM, summary.competitorType());
        assertEquals(CompetitorStatus.INJURED, summary.status());
    }

    @Test
    @DisplayName("toSummary with null returns null")
    void toSummary_withNull_returnsNull() {
        assertNull(CompetitorMapper.toSummary(null));
    }

    // ============================== toSummaries ==============================

    @Test
    @DisplayName("toSummaries converts a full list")
    void toSummaries_convertsList() {
        Competitor a = Competitor.builder().id(UUID.randomUUID()).name("A").nickname("A1")
                .competitorType(CompetitorType.DWARF).status(CompetitorStatus.ACTIVE).build();
        Competitor b = Competitor.builder().id(UUID.randomUUID()).name("B").nickname("B1")
                .competitorType(CompetitorType.CAMEL).status(CompetitorStatus.ACTIVE).build();

        List<CompetitorSummaryResponse> result = CompetitorMapper.toSummaries(List.of(a, b));

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).name());
        assertEquals("B", result.get(1).name());
    }

    @Test
    @DisplayName("toSummaries with null returns empty list")
    void toSummaries_withNull_returnsEmptyList() {
        assertTrue(CompetitorMapper.toSummaries(null).isEmpty());
    }
}