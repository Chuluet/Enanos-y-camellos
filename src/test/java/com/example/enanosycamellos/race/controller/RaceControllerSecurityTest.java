package com.example.enanosycamellos.race.controller;

import com.example.enanosycamellos.common.config.SecurityConfig;
import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.service.RaceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies REAL role-based authorization on RaceController, loading the
 * actual SecurityConfig (unlike RaceControllerTest, which disables security
 * filters on purpose to test the controller in isolation).
 *
 * <p>{@code @WithMockUser} injects an authenticated principal with the given
 * role directly into the SecurityContext for the test — no real JWT or
 * running Keycloak instance needed.</p>
 */
@WebMvcTest(RaceController.class)
@Import(SecurityConfig.class)
class RaceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaceService raceService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RaceRequest validRequest() {
        return new RaceRequest(
                "Byte's Revenge 1K", "Camel vs five dwarfs",
                LocalDateTime.now().plusDays(5),
                "North gate", "Trophy stand", 1000, 6,
                RaceType.MIXED, "organizer",
                LocalDateTime.now().plusDays(3)
        );
    }

    private RaceResponse sampleResponse() {
        return new RaceResponse(
                UUID.randomUUID(), "Byte's Revenge 1K", "Camel vs five dwarfs",
                LocalDateTime.now().plusDays(5), "North gate", "Trophy stand", 1000, 6,
                RaceType.MIXED, RaceStatus.DRAFT, "organizer",
                LocalDateTime.now().plusDays(3), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createRace_shouldReturn403_whenUserIsViewer() throws Exception {
        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void createRace_shouldReturn201_whenUserIsAdministrator() throws Exception {
        when(raceService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "RACE_ORGANIZER")
    void createRace_shouldReturn201_whenUserIsRaceOrganizer() throws Exception {
        when(raceService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void createRace_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getAllRaces_shouldReturn200_forViewer() throws Exception {
        mockMvc.perform(get("/api/races"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRaces_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/races"))
                .andExpect(status().isUnauthorized());
    }
}