package com.example.enanosycamellos.result.controller;

import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.result.dto.RaceResultRequest;
import com.example.enanosycamellos.result.dto.RaceResultResponse;
import com.example.enanosycamellos.result.dto.RaceResultUpdateRequest;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.result.service.RaceResultService;
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
 * Slice test: solo la capa web, con RaceResultService mockeado. Seguridad
 * desactivada a propósito, igual que en los demás *ControllerTest.
 */
@WebMvcTest(RaceResultController.class)
@AutoConfigureMockMvc(addFilters = false)
class RaceResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaceResultService resultService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RaceResultResponse sampleResponse(UUID id, UUID raceId, ResultStatus status, Integer finalPosition) {
        RaceSummaryResponse race = new RaceSummaryResponse(
                raceId, "Byte's Revenge 1K", RaceType.MIXED, RaceStatus.IN_PROGRESS, LocalDateTime.now());
        CompetitorSummaryResponse competitor = new CompetitorSummaryResponse(
                UUID.randomUUID(), "Byte", "byte-nick", CompetitorType.CAMEL, CompetitorStatus.ACTIVE);

        return new RaceResultResponse(
                id, UUID.randomUUID(), race, competitor, null,
                1, finalPosition, status == ResultStatus.FINISHED ? 120.0 : null, 0.0,
                status, null, "organizer", LocalDateTime.now()
        );
    }

    // ==================== GET /api/races/{raceId}/results ====================

    @Test
    void getResultsByRace_shouldReturn200_withList() throws Exception {
        UUID raceId = UUID.randomUUID();
        RaceResultResponse result = sampleResponse(UUID.randomUUID(), raceId, ResultStatus.FINISHED, 1);
        when(resultService.getByRace(raceId)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/races/{raceId}/results", raceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].finalPosition").value(1));
    }

    // ==================== POST /api/races/{raceId}/results ====================

    @Test
    void recordResult_shouldReturn201_whenRequestIsValid() throws Exception {
        UUID raceId = UUID.randomUUID();
        UUID generatedId = UUID.randomUUID();
        RaceResultRequest request = new RaceResultRequest(
                UUID.randomUUID(), null, 1, 120.0, null, ResultStatus.FINISHED, null, "organizer");

        when(resultService.record(any(RaceResultRequest.class)))
                .thenReturn(sampleResponse(generatedId, raceId, ResultStatus.FINISHED, 1));

        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/results/" + generatedId))
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void recordResult_shouldReturn400_whenRegistrationIdIsMissing() throws Exception {
        UUID raceId = UUID.randomUUID();
        RaceResultRequest invalidRequest = new RaceResultRequest(
                null, null, null, null, null, ResultStatus.DID_NOT_START, null, "organizer");

        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(resultService, never()).record(any());
    }

    @Test
    void recordResult_shouldReturn400_whenCompletionTimeIsNegative() throws Exception {
        UUID raceId = UUID.randomUUID();
        RaceResultRequest invalidRequest = new RaceResultRequest(
                UUID.randomUUID(), null, 1, -50.0, null, ResultStatus.FINISHED, null, "organizer");

        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(resultService, never()).record(any());
    }

    // ==================== GET /api/results/{id} ====================

    @Test
    void getById_shouldReturn200_whenResultExists() throws Exception {
        UUID id = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        when(resultService.getById(id)).thenReturn(sampleResponse(id, raceId, ResultStatus.FINISHED, 1));

        mockMvc.perform(get("/api/results/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    // ==================== PUT /api/results/{id} ====================

    @Test
    void updateResult_shouldReturn200_whenRequestIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        UUID raceId = UUID.randomUUID();
        RaceResultUpdateRequest request = new RaceResultUpdateRequest(
                1, 1, 118.0, null, ResultStatus.FINISHED, "adjusted", "organizer");

        when(resultService.update(eq(id), any(RaceResultUpdateRequest.class)))
                .thenReturn(sampleResponse(id, raceId, ResultStatus.FINISHED, 1));

        mockMvc.perform(put("/api/results/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void updateResult_shouldReturn400_whenStatusIsMissing() throws Exception {
        UUID id = UUID.randomUUID();
        RaceResultUpdateRequest invalidRequest = new RaceResultUpdateRequest(
                1, 1, 118.0, null, null, "adjusted", "organizer");

        mockMvc.perform(put("/api/results/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(resultService, never()).update(any(), any());
    }
}