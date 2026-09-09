package com.example.enanosycamellos.team.controller;

import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.team.dto.TeamPatchRequest;
import com.example.enanosycamellos.team.dto.TeamRequest;
import com.example.enanosycamellos.team.dto.TeamResponse;
import com.example.enanosycamellos.team.dto.TeamUpdateRequest;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.example.enanosycamellos.team.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for {@link TeamController}.
 * Uses @WebMvcTest to test the web layer in isolation, same approach as
 * CowControllerIntegrationTest.
 */
@WebMvcTest(TeamController.class)
// Estas pruebas miran el controller, no la seguridad: sin esta línea el filtro
// de Spring Security respondería 401 antes de que la petición llegue al método.
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TeamControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TeamService teamService;

    // ============================== GET ===============================

    @Nested
    @DisplayName("GET /api/teams")
    class GetAllTeams {

        @Test
        @DisplayName("returns 200 OK and a list of teams")
        void returnsOkAndList() throws Exception {
            TeamResponse response = buildTeamResponse(UUID.randomUUID(), "The Five Exceptions", List.of());
            when(teamService.getTeams()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/teams")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("The Five Exceptions"));

            verify(teamService).getTeams();
        }
    }

    @Nested
    @DisplayName("GET /api/teams/{id}")
    class GetTeamById {

        @Test
        @DisplayName("returns 200 OK if the team exists")
        void returnsOkIfFound() throws Exception {
            UUID id = UUID.randomUUID();
            TeamResponse response = buildTeamResponse(id, "The Five Exceptions", List.of());
            when(teamService.getById(id)).thenReturn(response);

            mockMvc.perform(get("/api/teams/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("The Five Exceptions"));
        }

        @Test
        @DisplayName("returns 404 Not Found if missing")
        void returnsNotFoundIfMissing() throws Exception {
            UUID id = UUID.randomUUID();
            when(teamService.getById(id)).thenThrow(ResourceNotFoundException.of("Team", id));

            mockMvc.perform(get("/api/teams/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("There is no Team with id " + id));
        }
    }

    // ============================== POST ===============================

    @Nested
    @DisplayName("POST /api/teams")
    class CreateTeam {

        @Test
        @DisplayName("returns 201 Created when valid")
        void returnsCreatedWhenValid() throws Exception {
            TeamRequest request = new TeamRequest(
                    "The Five Exceptions", "desc", "Mr. Abandonado", 5, TeamStatus.ACTIVE, null);
            UUID id = UUID.randomUUID();
            TeamResponse response = buildTeamResponse(id, "The Five Exceptions", List.of());

            when(teamService.create(any(TeamRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/teams/" + id))
                    .andExpect(jsonPath("$.name").value("The Five Exceptions"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when name is blank")
        void returnsBadRequestWhenInvalid() throws Exception {
            TeamRequest request = new TeamRequest("", null, "Mr. Abandonado", 5, null, null);

            mockMvc.perform(post("/api/teams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("There are invalid fields in the request"));

            verify(teamService, never()).create(any());
        }
    }

    // ============================== PUT ===============================

    @Nested
    @DisplayName("PUT /api/teams/{id}")
    class UpdateTeam {

        @Test
        @DisplayName("returns 200 OK when valid")
        void returnsOkWhenValid() throws Exception {
            UUID id = UUID.randomUUID();
            TeamUpdateRequest request = new TeamUpdateRequest(
                    "The Five Exceptions II", "new desc", "New Coach", 8, TeamStatus.ACTIVE);
            TeamResponse response = buildTeamResponse(id, "The Five Exceptions II", List.of());

            when(teamService.update(eq(id), any(TeamUpdateRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/teams/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("The Five Exceptions II"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when a mandatory field is missing")
        void returnsBadRequestWhenInvalid() throws Exception {
            // coach missing: PUT requires every field, unlike PATCH.
            String bodyMissingCoach = """
                    {"name":"The Five Exceptions","description":"desc","maxMembers":5,"status":"ACTIVE"}
                    """;

            mockMvc.perform(put("/api/teams/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyMissingCoach))
                    .andExpect(status().isBadRequest());

            verify(teamService, never()).update(any(), any());
        }
    }

    // ============================== PATCH ===============================

    @Nested
    @DisplayName("PATCH /api/teams/{id}")
    class PatchTeam {

        @Test
        @DisplayName("returns 200 OK when valid")
        void returnsOkWhenValid() throws Exception {
            UUID id = UUID.randomUUID();
            TeamPatchRequest request = new TeamPatchRequest("The Five Exceptions II", null, null, null, null);
            TeamResponse response = buildTeamResponse(id, "The Five Exceptions II", List.of());

            when(teamService.patch(eq(id), any(TeamPatchRequest.class))).thenReturn(response);

            mockMvc.perform(patch("/api/teams/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("The Five Exceptions II"));
        }
    }

    // ============================== Member management ===============================

    @Nested
    @DisplayName("POST /api/teams/{teamId}/members/{competitorId}")
    class AddMember {

        @Test
        @DisplayName("returns 200 OK when the competitor is added")
        void returnsOkWhenValid() throws Exception {
            UUID teamId = UUID.randomUUID();
            UUID competitorId = UUID.randomUUID();
            CompetitorSummaryResponse member = new CompetitorSummaryResponse(
                    competitorId, "Null Pointer", "NP", CompetitorType.DWARF, CompetitorStatus.ACTIVE);
            TeamResponse response = buildTeamResponse(teamId, "The Five Exceptions", List.of(member));

            when(teamService.addMember(teamId, competitorId)).thenReturn(response);

            mockMvc.perform(post("/api/teams/{teamId}/members/{competitorId}", teamId, competitorId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.members[0].nickname").value("NP"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/teams/{teamId}/members/{competitorId}")
    class RemoveMember {

        @Test
        @DisplayName("returns 200 OK when the competitor is removed")
        void returnsOkWhenValid() throws Exception {
            UUID teamId = UUID.randomUUID();
            UUID competitorId = UUID.randomUUID();
            TeamResponse response = buildTeamResponse(teamId, "The Five Exceptions", List.of());

            when(teamService.removeMember(teamId, competitorId)).thenReturn(response);

            mockMvc.perform(delete("/api/teams/{teamId}/members/{competitorId}", teamId, competitorId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.members").isEmpty());
        }
    }

    // ============================== DELETE (deactivate) ===============================

    @Nested
    @DisplayName("DELETE /api/teams/{id}")
    class DeleteTeam {

        @Test
        @DisplayName("returns 204 No Content")
        void returnsNoContent() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(teamService).deactivate(id);

            mockMvc.perform(delete("/api/teams/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(teamService).deactivate(id);
        }
    }

    // ============================== Helpers ===============================

    private TeamResponse buildTeamResponse(UUID id, String name, List<CompetitorSummaryResponse> members) {
        return new TeamResponse(
                id, name, "desc", "Mr. Abandonado", LocalDate.now(),
                TeamStatus.ACTIVE, 0, 0, 5, members);
    }
}