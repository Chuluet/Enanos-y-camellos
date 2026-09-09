package com.example.enanosycamellos.team.mapper;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.team.dto.TeamRequest;
import com.example.enanosycamellos.team.dto.TeamResponse;
import com.example.enanosycamellos.team.dto.TeamSummaryResponse;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the team mapper. Pure tests: no Spring, no mocks — the
 * mapper is a stateless utility class, same approach as CowMapperTest.
 */
class TeamMapperTest {

    // ============================== toEntity ==============================

    @Test
    @DisplayName("toEntity maps name, description, coach and maxMembers")
    void toEntity_mapsFields() {
        TeamRequest request = new TeamRequest(
                "The Five Exceptions", "Five dwarfs against the odds", "Mr. Abandonado",
                5, TeamStatus.ACTIVE, null);

        Team team = TeamMapper.toEntity(request);

        assertNotNull(team);
        assertEquals("The Five Exceptions", team.getName());
        assertEquals("Five dwarfs against the odds", team.getDescription());
        assertEquals("Mr. Abandonado", team.getCoach());
        assertEquals(5, team.getMaxMembers());
        assertEquals(TeamStatus.ACTIVE, team.getStatus());
        // Members are NOT resolved by the mapper, but by the service.
        assertTrue(team.getMembers().isEmpty());
    }

    @Test
    @DisplayName("toEntity defaults status to ACTIVE when not sent")
    void toEntity_defaultsStatus() {
        TeamRequest request = new TeamRequest(
                "The Five Exceptions", null, "Mr. Abandonado", 5, null, null);

        Team team = TeamMapper.toEntity(request);

        assertEquals(TeamStatus.ACTIVE, team.getStatus());
    }

    @Test
    @DisplayName("toEntity with null returns null")
    void toEntity_withNull_returnsNull() {
        assertNull(TeamMapper.toEntity(null));
    }

    // ============================== toResponse ==============================

    @Test
    @DisplayName("toResponse maps all fields, including members as summaries")
    void toResponse_mapsAllFields() {
        UUID teamId = UUID.randomUUID();
        Team team = Team.builder()
                .id(teamId).name("The Five Exceptions").description("desc")
                .coach("Mr. Abandonado").status(TeamStatus.ACTIVE)
                .victories(3).defeats(1).maxMembers(5)
                .build();

        Competitor member = Competitor.builder()
                .id(UUID.randomUUID()).name("Null Pointer").nickname("NP")
                .competitorType(CompetitorType.DWARF).status(CompetitorStatus.ACTIVE)
                .build();
        team.addMember(member);

        TeamResponse response = TeamMapper.toResponse(team);

        assertNotNull(response);
        assertEquals(teamId, response.id());
        assertEquals("The Five Exceptions", response.name());
        assertEquals("desc", response.description());
        assertEquals("Mr. Abandonado", response.coach());
        assertEquals(TeamStatus.ACTIVE, response.status());
        assertEquals(3, response.victories());
        assertEquals(1, response.defeats());
        assertEquals(5, response.maxMembers());
        assertEquals(1, response.members().size());
        assertEquals("Null Pointer", response.members().getFirst().name());
    }

    @Test
    @DisplayName("toResponse with null returns null")
    void toResponse_withNull_returnsNull() {
        assertNull(TeamMapper.toResponse(null));
    }

    // ============================== toSummary ==============================

    @Test
    @DisplayName("toSummary maps id, name and status")
    void toSummary_mapsIdNameAndStatus() {
        UUID id = UUID.randomUUID();
        Team team = Team.builder().id(id).name("The Five Exceptions").status(TeamStatus.SUSPENDED).build();

        TeamSummaryResponse summary = TeamMapper.toSummary(team);

        assertNotNull(summary);
        assertEquals(id, summary.id());
        assertEquals("The Five Exceptions", summary.name());
        assertEquals(TeamStatus.SUSPENDED, summary.status());
    }

    @Test
    @DisplayName("toSummary with null returns null")
    void toSummary_withNull_returnsNull() {
        assertNull(TeamMapper.toSummary(null));
    }

    // ============================== toSummaries ==============================

    @Test
    @DisplayName("toSummaries converts a full list")
    void toSummaries_convertsList() {
        Team a = Team.builder().id(UUID.randomUUID()).name("A").status(TeamStatus.ACTIVE).build();
        Team b = Team.builder().id(UUID.randomUUID()).name("B").status(TeamStatus.INACTIVE).build();

        List<TeamSummaryResponse> result = TeamMapper.toSummaries(List.of(a, b));

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).name());
        assertEquals("B", result.get(1).name());
    }

    @Test
    @DisplayName("toSummaries with null returns empty list")
    void toSummaries_withNull_returnsEmptyList() {
        assertTrue(TeamMapper.toSummaries(null).isEmpty());
    }
}