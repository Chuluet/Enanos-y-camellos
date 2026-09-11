package com.example.enanosycamellos.result.controller;

import com.example.enanosycamellos.common.config.SecurityConfig;
import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.result.dto.RaceResultRequest;
import com.example.enanosycamellos.result.dto.RaceResultResponse;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.result.service.RaceResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RaceResultController.class)
@Import(SecurityConfig.class)
class RaceResultControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaceResultService resultService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UUID raceId = UUID.randomUUID();

    private RaceResultRequest validRequest() {
        return new RaceResultRequest(
                UUID.randomUUID(), null, 1, 120.0, null, ResultStatus.FINISHED, null, "organizer");
    }

    private RaceResultResponse sampleResponse() {
        RaceSummaryResponse race = new RaceSummaryResponse(
                raceId, "Byte's Revenge 1K", RaceType.MIXED, RaceStatus.IN_PROGRESS, LocalDateTime.now());
        CompetitorSummaryResponse competitor = new CompetitorSummaryResponse(
                UUID.randomUUID(), "Byte", "byte-nick", CompetitorType.CAMEL, CompetitorStatus.ACTIVE);
        return new RaceResultResponse(
                UUID.randomUUID(), UUID.randomUUID(), race, competitor, null,
                1, 1, 120.0, 0.0, ResultStatus.FINISHED, null, "organizer", LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void recordResult_shouldReturn403_whenUserIsViewer() throws Exception {
        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void recordResult_shouldReturn201_whenUserIsAdministrator() throws Exception {
        when(resultService.record(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "RACE_ORGANIZER")
    void recordResult_shouldReturn201_whenUserIsRaceOrganizer() throws Exception {
        when(resultService.record(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getResultsByRace_shouldReturn200_forViewer() throws Exception {
        mockMvc.perform(get("/api/races/{raceId}/results", raceId))
                .andExpect(status().isOk());
    }

    @Test
    void recordResult_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }
}