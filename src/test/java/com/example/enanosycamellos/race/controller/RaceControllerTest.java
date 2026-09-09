package com.example.enanosycamellos.race.controller;

import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.dto.RaceStatusUpdateRequest;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.service.RaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test: levanta solo la capa web (controller + validación), con
 * RaceService mockeado. Seguridad desactivada a propósito: eso lo prueba
 * quien construya el Módulo 1, acá solo nos interesa el comportamiento
 * HTTP del controller.
 */
@WebMvcTest(RaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class RaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RaceService raceService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RaceResponse sampleResponse(UUID id, RaceStatus status) {
        return new RaceResponse(
                id,
                "Byte's Revenge 1K",
                "Camel vs five dwarfs",
                LocalDateTime.now().plusDays(5),
                "North gate",
                "Trophy stand",
                1000,
                6,
                RaceType.MIXED,
                status,
                "mr.abandonado",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ================================ GET /api/races =============================

    @Test
    void getAllRaces_shouldReturn200_withList() throws Exception {
        RaceResponse race = sampleResponse(UUID.randomUUID(), RaceStatus.DRAFT);
        when(raceService.getRaces(null, null)).thenReturn(List.of(race));

        mockMvc.perform(get("/api/races"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Byte's Revenge 1K"));
    }

    @Test
    void getAllRaces_shouldPassFilters_whenQueryParamsAreGiven() throws Exception {
        when(raceService.getRaces(RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/races")
                        .param("status", "OPEN_FOR_REGISTRATION")
                        .param("type", "MIXED"))
                .andExpect(status().isOk());

        verify(raceService).getRaces(RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED);
    }

    // ============================ GET /api/races/{id} ============================

    @Test
    void getRaceById_shouldReturn200_whenRaceExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(raceService.getById(id)).thenReturn(sampleResponse(id, RaceStatus.DRAFT));

        mockMvc.perform(get("/api/races/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
        // ================================ POST /api/races =============================

    @Test
    void createRace_shouldReturn201_whenRequestIsValid() throws Exception {
        RaceRequest request = new RaceRequest(
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
        UUID generatedId = UUID.randomUUID();
        when(raceService.create(any(RaceRequest.class)))
                .thenReturn(sampleResponse(generatedId, RaceStatus.DRAFT));

        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/races/" + generatedId))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createRace_shouldReturn400_whenNameIsBlank() throws Exception {
        RaceRequest invalidRequest = new RaceRequest(
                "", // nombre vacío: viola @NotBlank
                "desc",
                LocalDateTime.now().plusDays(5),
                "start",
                "finish",
                1000,
                6,
                RaceType.INDIVIDUAL,
                "organizer",
                LocalDateTime.now().plusDays(3)
        );

        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(raceService, never()).create(any());
    }

    @Test
    void createRace_shouldReturn400_whenDistanceIsNegative() throws Exception {
        RaceRequest invalidRequest = new RaceRequest(
                "Valid name",
                "desc",
                LocalDateTime.now().plusDays(5),
                "start",
                "finish",
                -100, // viola @Positive
                6,
                RaceType.INDIVIDUAL,
                "organizer",
                LocalDateTime.now().plusDays(3)
        );

        mockMvc.perform(post("/api/races")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(raceService, never()).create(any());
    }

    // ============================ PATCH /api/races/{id}/status ============================

    @Test
    void changeStatus_shouldReturn200_whenTransitionIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        RaceStatusUpdateRequest request = new RaceStatusUpdateRequest(RaceStatus.OPEN_FOR_REGISTRATION);

        when(raceService.changeStatus(id, RaceStatus.OPEN_FOR_REGISTRATION))
                .thenReturn(sampleResponse(id, RaceStatus.OPEN_FOR_REGISTRATION));

        mockMvc.perform(patch("/api/races/{id}/status", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN_FOR_REGISTRATION"));
    }

    @Test
    void changeStatus_shouldReturn400_whenStatusIsMissing() throws Exception {
        String bodyWithoutStatus = "{}";

        mockMvc.perform(patch("/api/races/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(bodyWithoutStatus))
                .andExpect(status().isBadRequest());

        verify(raceService, never()).changeStatus(any(), any());
    }

    // ================================ DELETE /api/races/{id} =============================

    @Test
    void cancelRace_shouldReturn204_whenSuccessful() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(raceService).cancel(id);

        mockMvc.perform(delete("/api/races/{id}", id))
                .andExpect(status().isNoContent());

        verify(raceService).cancel(id);
    }
    
}