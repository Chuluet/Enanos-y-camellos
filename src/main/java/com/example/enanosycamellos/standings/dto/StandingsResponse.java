package com.example.enanosycamellos.standings.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Combined league standings: competitors and teams, each sorted by points descending")
public record StandingsResponse(
        List<CompetitorStandingResponse> competitors,
        List<TeamStandingResponse> teams
) {
}