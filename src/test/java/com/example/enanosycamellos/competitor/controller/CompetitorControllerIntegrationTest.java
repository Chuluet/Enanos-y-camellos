package com.example.enanosycamellos.competitor.controller;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.dto.CompetitorPatchRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorResponse;
import com.example.enanosycamellos.competitor.dto.CompetitorStatusUpdateRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorUpdateRequest;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.competitor.service.CompetitorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for {@link CompetitorController}.
 * Same {@code @WebMvcTest} approach as {@code TeamControllerIntegrationTest}:
 * security filters disabled here (role-based access, if any, is exercised in
 * a full-context security test elsewhere). Request bodies are raw JSON text
 * blocks rather than serialized record instances, so these tests don't
 * depend on the exact constructor order of the DTOs.
 */
@WebMvcTest(CompetitorController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CompetitorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompetitorService competitorService;

    // ============================== GET /api/competitors ===============================

    @Nested
    @DisplayName("GET /api/competitors")
    class GetAllCompetitors {

        @Test
        @DisplayName("without filters, returns 200 OK with a page of competitors")
        void returnsOkAndPage() throws Exception {
            CompetitorResponse response = buildResponse(UUID.randomUUID(), "John D", "JD", CompetitorStatus.ACTIVE);
            Page<CompetitorResponse> page = new PageImpl<>(List.of(response));
            when(competitorService.getCompetitors(isNull(), isNull(), any())).thenReturn(page);

            mockMvc.perform(get("/api/competitors")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].nickname").value("JD"));
        }

        @Test
        @DisplayName("with a status filter, passes it through to the service")
        void passesStatusFilter() throws Exception {
            CompetitorResponse response = buildResponse(UUID.randomUUID(), "Retired One", "RO", CompetitorStatus.RETIRED);
            Page<CompetitorResponse> page = new PageImpl<>(List.of(response));
            when(competitorService.getCompetitors(eq(CompetitorStatus.RETIRED), isNull(), any())).thenReturn(page);

            mockMvc.perform(get("/api/competitors").param("status", "RETIRED")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("RETIRED"));

            verify(competitorService).getCompetitors(eq(CompetitorStatus.RETIRED), isNull(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/competitors/{id}")
    class GetCompetitorById {

        @Test
        @DisplayName("returns 200 OK if the competitor exists")
        void returnsOkIfFound() throws Exception {
            UUID id = UUID.randomUUID();
            CompetitorResponse response = buildResponse(id, "John D", "JD", CompetitorStatus.ACTIVE);
            when(competitorService.getById(id)).thenReturn(response);

            mockMvc.perform(get("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("JD"));
        }

        @Test
        @DisplayName("returns 404 Not Found if missing")
        void returnsNotFoundIfMissing() throws Exception {
            UUID id = UUID.randomUUID();
            when(competitorService.getById(id)).thenThrow(ResourceNotFoundException.of("Competitor", id));

            mockMvc.perform(get("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("There is no Competitor with id " + id));
        }
    }

    // ============================== POST /api/competitors ===============================

    @Nested
    @DisplayName("POST /api/competitors")
    class CreateCompetitor {

        @Test
        @DisplayName("returns 201 Created when valid")
        void returnsCreatedWhenValid() throws Exception {
            UUID id = UUID.randomUUID();
            CompetitorResponse response = buildResponse(id, "John D", "JD", CompetitorStatus.ACTIVE);
            when(competitorService.create(any(CompetitorRequest.class))).thenReturn(response);

            String body = """
                    {
                      "name": "John D",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 120.0,
                      "weight": 50.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(post("/api/competitors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/competitors/" + id))
                    .andExpect(jsonPath("$.nickname").value("JD"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when name is blank")
        void returnsBadRequestWhenNameBlank() throws Exception {
            String body = """
                    {
                      "name": "",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 120.0,
                      "weight": 50.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(post("/api/competitors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(competitorService, never()).create(any());
        }

        @Test
        @DisplayName("returns 400 Bad Request when weight is not positive")
        void returnsBadRequestWhenWeightInvalid() throws Exception {
            String body = """
                    {
                      "name": "John D",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 120.0,
                      "weight": -5.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(post("/api/competitors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(competitorService, never()).create(any());
        }

        @Test
        @DisplayName("returns 400 Bad Request when both dateOfBirth and approximateAge are missing")
        void returnsBadRequestWhenAgeInfoMissing() throws Exception {
            when(competitorService.create(any(CompetitorRequest.class)))
                    .thenThrow(new BadRequestException("Either dateOfBirth or approximateAge must be provided"));

            String body = """
                    {
                      "name": "John D",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "height": 120.0,
                      "weight": 50.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(post("/api/competitors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 Conflict when the nickname is already taken")
        void returnsConflictWhenDuplicateNickname() throws Exception {
            when(competitorService.create(any(CompetitorRequest.class)))
                    .thenThrow(new ConflictException("There is already a competitor nicknamed 'JD'"));

            String body = """
                    {
                      "name": "John D",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 120.0,
                      "weight": 50.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(post("/api/competitors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("There is already a competitor nicknamed 'JD'"));
        }
    }

    // ============================== PUT /api/competitors/{id} ===============================

    @Nested
    @DisplayName("PUT /api/competitors/{id}")
    class UpdateCompetitor {

        @Test
        @DisplayName("returns 200 OK when valid")
        void returnsOkWhenValid() throws Exception {
            UUID id = UUID.randomUUID();
            CompetitorResponse response = buildResponse(id, "John D II", "JD2", CompetitorStatus.ACTIVE);
            when(competitorService.update(eq(id), any(CompetitorUpdateRequest.class))).thenReturn(response);

            String body = """
                    {
                      "name": "John D II",
                      "nickname": "JD2",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 121.0,
                      "weight": 51.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(put("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("JD2"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when weight is not positive")
        void returnsBadRequestWhenWeightInvalid() throws Exception {
            UUID id = UUID.randomUUID();
            String body = """
                    {
                      "name": "John D",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 120.0,
                      "weight": 0.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(put("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verify(competitorService, never()).update(any(), any());
        }

        @Test
        @DisplayName("returns 404 Not Found if the competitor does not exist")
        void returnsNotFoundIfMissing() throws Exception {
            UUID id = UUID.randomUUID();
            when(competitorService.update(eq(id), any(CompetitorUpdateRequest.class)))
                    .thenThrow(ResourceNotFoundException.of("Competitor", id));

            String body = """
                    {
                      "name": "John D",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 120.0,
                      "weight": 50.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(put("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 Conflict when the new nickname is already taken")
        void returnsConflictWhenDuplicateNickname() throws Exception {
            UUID id = UUID.randomUUID();
            when(competitorService.update(eq(id), any(CompetitorUpdateRequest.class)))
                    .thenThrow(new ConflictException("There is already another competitor nicknamed 'JD'"));

            String body = """
                    {
                      "name": "John D",
                      "nickname": "JD",
                      "competitorType": "DWARF",
                      "dateOfBirth": "1989-06-15",
                      "height": 120.0,
                      "weight": 50.0,
                      "origin": "USA"
                    }
                    """;

            mockMvc.perform(put("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    // ============================== PATCH /api/competitors/{id}/status ===============================

    @Nested
    @DisplayName("PATCH /api/competitors/{id}/status")
    class ChangeStatus {

        @Test
        @DisplayName("returns 200 OK when valid")
        void returnsOkWhenValid() throws Exception {
            UUID id = UUID.randomUUID();
            CompetitorResponse response = buildResponse(id, "John D", "JD", CompetitorStatus.INJURED);
            when(competitorService.changeStatus(eq(id), any(CompetitorStatusUpdateRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/api/competitors/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "INJURED"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("INJURED"));
        }

        @Test
        @DisplayName("returns 409 Conflict when the competitor is retired (terminal state)")
        void returnsConflictWhenRetired() throws Exception {
            UUID id = UUID.randomUUID();
            when(competitorService.changeStatus(eq(id), any(CompetitorStatusUpdateRequest.class)))
                    .thenThrow(new ConflictException(
                            "Competitor 'JD' has retired and cannot be reactivated or change status"));

            mockMvc.perform(patch("/api/competitors/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "ACTIVE"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 Bad Request when status is missing")
        void returnsBadRequestWhenStatusMissing() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(patch("/api/competitors/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(competitorService, never()).changeStatus(any(), any());
        }
    }

    // ============================== PATCH /api/competitors/{id}/retire ===============================

    @Nested
    @DisplayName("PATCH /api/competitors/{id}/retire")
    class RetireCompetitor {

        @Test
        @DisplayName("returns 204 No Content when valid")
        void returnsNoContent() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(competitorService).retire(id);

            mockMvc.perform(patch("/api/competitors/{id}/retire", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(competitorService).retire(id);
        }

        @Test
        @DisplayName("returns 409 Conflict when already retired")
        void returnsConflictWhenAlreadyRetired() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new ConflictException("Competitor 'JD' has already retired"))
                    .when(competitorService).retire(id);

            mockMvc.perform(patch("/api/competitors/{id}/retire", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 404 Not Found if the competitor does not exist")
        void returnsNotFoundIfMissing() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(ResourceNotFoundException.of("Competitor", id))
                    .when(competitorService).retire(id);

            mockMvc.perform(patch("/api/competitors/{id}/retire", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================== DELETE /api/competitors/{id} ===============================

    @Nested
    @DisplayName("DELETE /api/competitors/{id}")
    class DeleteCompetitor {

        @Test
        @DisplayName("returns 204 No Content when the competitor has no official results")
        void returnsNoContent() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(competitorService).delete(id);

            mockMvc.perform(delete("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(competitorService).delete(id);
        }

        @Test
        @DisplayName("returns 409 Conflict when the competitor has official race results")
        void returnsConflictWhenHasResults() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new ConflictException(
                    "Competitor 'JD' has official race results and cannot be permanently deleted; retire them instead"))
                    .when(competitorService).delete(id);

            mockMvc.perform(delete("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 404 Not Found if the competitor does not exist")
        void returnsNotFoundIfMissing() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(ResourceNotFoundException.of("Competitor", id))
                    .when(competitorService).delete(id);

            mockMvc.perform(delete("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================== PATCH /api/competitors/{id} ===============================

    @Nested
    @DisplayName("PATCH /api/competitors/{id}")
    class PatchCompetitor {

        @Test
        @DisplayName("returns 200 OK when valid")
        void returnsOkWhenValid() throws Exception {
            UUID id = UUID.randomUUID();
            CompetitorResponse response = buildResponse(id, "John D", "JD2", CompetitorStatus.ACTIVE);
            when(competitorService.patch(eq(id), any(CompetitorPatchRequest.class))).thenReturn(response);

            mockMvc.perform(patch("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "JD2"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("JD2"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when the request is empty")
        void returnsBadRequestWhenEmpty() throws Exception {
            UUID id = UUID.randomUUID();
            when(competitorService.patch(eq(id), any(CompetitorPatchRequest.class)))
                    .thenThrow(new BadRequestException("The request has no fields to update"));

            mockMvc.perform(patch("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 Conflict when the new nickname is already taken")
        void returnsConflictWhenDuplicateNickname() throws Exception {
            UUID id = UUID.randomUUID();
            when(competitorService.patch(eq(id), any(CompetitorPatchRequest.class)))
                    .thenThrow(new ConflictException("There is already another competitor nicknamed 'JD'"));

            mockMvc.perform(patch("/api/competitors/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "JD"}
                                    """))
                    .andExpect(status().isConflict());
        }
    }

    // ============================== Helpers ===============================

    private CompetitorResponse buildResponse(UUID id, String name, String nickname, CompetitorStatus status) {
        return new CompetitorResponse(
                id, name, nickname, CompetitorType.DWARF,
                LocalDate.of(1989, 6, 15), null, 120.0, 50.0, "USA",
                status, LocalDate.now(), 0, 0, 0, null);
    }
}