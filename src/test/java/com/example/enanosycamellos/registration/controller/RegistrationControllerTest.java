package com.example.enanosycamellos.registration.controller;

import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.registration.dto.RaceRegistrationRequest;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.dto.RegistrationRejectRequest;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.registration.service.RegistrationService;
import com.example.enanosycamellos.team.dto.TeamSummaryResponse;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Slice test: solo la capa web (controller + validación de DTOs), con
 * RegistrationService mockeado. Seguridad desactivada a propósito, igual
 * que en RaceControllerTest — eso lo prueba quien construya el Módulo 1.
 */
@WebMvcTest(RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RaceSummaryResponse sampleRace(UUID id) {
        return new RaceSummaryResponse(id, "Byte's Revenge 1K", RaceType.MIXED,
                RaceStatus.OPEN_FOR_REGISTRATION, LocalDateTime.now().plusDays(5));
    }

    private CompetitorSummaryResponse sampleCompetitor(UUID id) {
        return new CompetitorSummaryResponse(id, "Byte", "byte-nick",
                CompetitorType.CAMEL, CompetitorStatus.ACTIVE);
    }

    private TeamSummaryResponse sampleTeam(UUID id) {
        return new TeamSummaryResponse(id, "The Five Exceptions", TeamStatus.ACTIVE);
    }

    private RaceRegistrationResponse sampleIndividualResponse(
            UUID id, UUID raceId, UUID competitorId, RegistrationStatus status) {
        return new RaceRegistrationResponse(
                id,
                sampleRace(raceId),
                sampleCompetitor(competitorId),
                null,
                LocalDateTime.now(),
                status,
                null,
                null,
                "organizer"
        );
    }

    // ==================== GET /api/races/{raceId}/registrations ====================

    @Test
    void getRegistrationsByRace_shouldReturn200_withList() throws Exception {
        UUID raceId = UUID.randomUUID();
        RaceRegistrationResponse registration =
                sampleIndividualResponse(UUID.randomUUID(), raceId, UUID.randomUUID(), RegistrationStatus.PENDING);
        when(registrationService.getByRace(raceId)).thenReturn(List.of(registration));

        mockMvc.perform(get("/api/races/{raceId}/registrations", raceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ==================== POST /api/races/{raceId}/registrations ====================

    @Test
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        UUID raceId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        UUID generatedId = UUID.randomUUID();
        RaceRegistrationRequest request = new RaceRegistrationRequest(competitorId, null, null, "organizer");

        when(registrationService.register(eq(raceId), any(RaceRegistrationRequest.class)))
                .thenReturn(sampleIndividualResponse(generatedId, raceId, competitorId, RegistrationStatus.PENDING));

        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/registrations/" + generatedId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void register_shouldReturn400_whenRegisteredByIsBlank() throws Exception {
        UUID raceId = UUID.randomUUID();
        RaceRegistrationRequest invalidRequest =
                new RaceRegistrationRequest(UUID.randomUUID(), null, null, "");

        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any(), any());
    }

    @Test
    void register_shouldReturn400_whenStartingPositionIsNegative() throws Exception {
        UUID raceId = UUID.randomUUID();
        RaceRegistrationRequest invalidRequest =
                new RaceRegistrationRequest(UUID.randomUUID(), null, -1, "organizer");

        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).register(any(), any());
    }

    // ==================== GET /api/registrations/{id} ====================

    @Test
    void getById_shouldReturn200_whenRegistrationExists() throws Exception {
        UUID id = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        when(registrationService.getById(id))
                .thenReturn(sampleIndividualResponse(id, raceId, competitorId, RegistrationStatus.APPROVED));

        mockMvc.perform(get("/api/registrations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // ==================== PATCH /api/registrations/{id}/approve ====================

    @Test
    void approve_shouldReturn200_whenSuccessful() throws Exception {
        UUID id = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        when(registrationService.approve(id))
                .thenReturn(sampleIndividualResponse(id, raceId, competitorId, RegistrationStatus.APPROVED));

        mockMvc.perform(patch("/api/registrations/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // ==================== PATCH /api/registrations/{id}/reject ====================

    @Test
    void reject_shouldReturn200_whenReasonIsProvided() throws Exception {
        UUID id = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        RegistrationRejectRequest request = new RegistrationRejectRequest("Competitor is SUSPENDED");

        when(registrationService.reject(id, "Competitor is SUSPENDED"))
                .thenReturn(sampleIndividualResponse(id, raceId, competitorId, RegistrationStatus.REJECTED));

        mockMvc.perform(patch("/api/registrations/{id}/reject", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void reject_shouldReturn400_whenReasonIsMissing() throws Exception {
        UUID id = UUID.randomUUID();
        String bodyWithoutReason = "{}";

        mockMvc.perform(patch("/api/registrations/{id}/reject", id)
                        .contentType("application/json")
                        .content(bodyWithoutReason))
                .andExpect(status().isBadRequest());

        verify(registrationService, never()).reject(any(), any());
    }

    // ==================== DELETE /api/registrations/{id} ====================

    @Test
    void cancel_shouldReturn204_whenSuccessful() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(registrationService).cancel(id);

        mockMvc.perform(delete("/api/registrations/{id}", id))
                .andExpect(status().isNoContent());

        verify(registrationService).cancel(id);
    }
}