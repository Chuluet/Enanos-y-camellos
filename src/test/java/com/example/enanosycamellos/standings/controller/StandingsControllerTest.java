package com.example.enanosycamellos.standings.controller;

import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.standings.dto.CompetitorStandingResponse;
import com.example.enanosycamellos.standings.dto.StandingsResponse;
import com.example.enanosycamellos.standings.dto.TeamStandingResponse;
import com.example.enanosycamellos.standings.service.StandingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test para los 3 endpoints de solo lectura de Standings. Seguridad
 * desactivada a propósito, igual que en los demás *ControllerTest.
 */
@WebMvcTest(StandingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StandingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StandingsService standingsService;

    private CompetitorStandingResponse sampleCompetitorStanding() {
        return new CompetitorStandingResponse(
                UUID.randomUUID(), "Byte", CompetitorType.CAMEL, 10, 1, 0, 1);
    }

    private TeamStandingResponse sampleTeamStanding() {
        return new TeamStandingResponse(UUID.randomUUID(), "The Five Exceptions", 7, 1, 0);
    }

    @Test
    void getStandings_shouldReturn200_withCombinedStandings() throws Exception {
        StandingsResponse combined = new StandingsResponse(
                List.of(sampleCompetitorStanding()), List.of(sampleTeamStanding()));
        when(standingsService.getStandings()).thenReturn(combined);

        mockMvc.perform(get("/api/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competitors[0].nickname").value("Byte"))
                .andExpect(jsonPath("$.teams[0].name").value("The Five Exceptions"));
    }

    @Test
    void getCompetitorStandings_shouldReturn200_withList() throws Exception {
        when(standingsService.getCompetitorStandings()).thenReturn(List.of(sampleCompetitorStanding()));

        mockMvc.perform(get("/api/standings/competitors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalPoints").value(10));
    }

    @Test
    void getTeamStandings_shouldReturn200_withList() throws Exception {
        when(standingsService.getTeamStandings()).thenReturn(List.of(sampleTeamStanding()));

        mockMvc.perform(get("/api/standings/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalPoints").value(7));
    }
}