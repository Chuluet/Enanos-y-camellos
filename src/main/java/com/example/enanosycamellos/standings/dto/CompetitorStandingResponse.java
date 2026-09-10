package com.example.enanosycamellos.standings.dto;

import com.example.enanosycamellos.competitor.entity.CompetitorType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A competitor's position in the league standings")
public record CompetitorStandingResponse(
        UUID competitorId,
        String nickname,
        CompetitorType competitorType,
        int totalPoints,
        int victories,
        int defeats,
        int completedRaces
) {
}