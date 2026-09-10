package com.example.enanosycamellos.standings.controller;

import com.example.enanosycamellos.standings.dto.CompetitorStandingResponse;
import com.example.enanosycamellos.standings.dto.StandingsResponse;
import com.example.enanosycamellos.standings.dto.TeamStandingResponse;
import com.example.enanosycamellos.standings.service.StandingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
@Tag(name = "Standings", description = "League standings, computed from recorded results")
public class StandingsController {

    private final StandingsService standingsService;

    @GetMapping
    @Operation(summary = "Combined league standings (competitors and teams)")
    @ApiResponse(responseCode = "200", description = "Standings obtained")
    public ResponseEntity<StandingsResponse> getStandings() {
        return ResponseEntity.ok(standingsService.getStandings());
    }

    @GetMapping("/competitors")
    @Operation(summary = "Competitor standings, sorted by points descending")
    @ApiResponse(responseCode = "200", description = "Standings obtained")
    public ResponseEntity<List<CompetitorStandingResponse>> getCompetitorStandings() {
        return ResponseEntity.ok(standingsService.getCompetitorStandings());
    }

    @GetMapping("/teams")
    @Operation(summary = "Team standings, sorted by points descending")
    @ApiResponse(responseCode = "200", description = "Standings obtained")
    public ResponseEntity<List<TeamStandingResponse>> getTeamStandings() {
        return ResponseEntity.ok(standingsService.getTeamStandings());
    }
}