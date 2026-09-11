package com.example.enanosycamellos.registration.controller;

import com.example.enanosycamellos.common.config.SecurityConfig;
import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.registration.dto.RaceRegistrationRequest;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.registration.service.RegistrationService;
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

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
class RegistrationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UUID raceId = UUID.randomUUID();

    private RaceRegistrationRequest validRequest() {
        return new RaceRegistrationRequest(UUID.randomUUID(), null, null, "organizer");
    }

    private RaceRegistrationResponse sampleResponse() {
        RaceSummaryResponse race = new RaceSummaryResponse(
                raceId, "Byte's Revenge 1K", RaceType.MIXED, RaceStatus.OPEN_FOR_REGISTRATION, LocalDateTime.now());
        CompetitorSummaryResponse competitor = new CompetitorSummaryResponse(
                UUID.randomUUID(), "Byte", "byte-nick", CompetitorType.CAMEL, CompetitorStatus.ACTIVE);
        return new RaceRegistrationResponse(
                UUID.randomUUID(), race, competitor, null,
                LocalDateTime.now(), RegistrationStatus.PENDING, null, null, "organizer");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void register_shouldReturn403_whenUserIsViewer() throws Exception {
        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void register_shouldReturn201_whenUserIsAdministrator() throws Exception {
        when(registrationService.register(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "RACE_ORGANIZER")
    void approve_shouldReturn200_whenUserIsRaceOrganizer() throws Exception {
        UUID id = UUID.randomUUID();
        when(registrationService.approve(id)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/registrations/{id}/approve", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void approve_shouldReturn403_whenUserIsViewer() throws Exception {
        mockMvc.perform(patch("/api/registrations/{id}/approve", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getRegistrationsByRace_shouldReturn200_forViewer() throws Exception {
        mockMvc.perform(get("/api/races/{raceId}/registrations", raceId))
                .andExpect(status().isOk());
    }

    @Test
    void register_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }
}