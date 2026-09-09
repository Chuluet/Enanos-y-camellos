package com.example.enanosycamellos.competitor.dto;

import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Short version of a competitor, for when it appears inside another resource
 * (the member list of a team).
 *
 * <p>Breaks the recursion: the full {@code CompetitorResponse} carries its
 * team, which would carry its members, which would carry their teams... </p>
 */
@Schema(description = "Minimal competitor data, used inside other resources")
public record CompetitorSummaryResponse(

        @Schema(description = "Competitor identifier")
        UUID id,

        @Schema(description = "Competitor name", example = "Marvin Alberto")
        String name,

        @Schema(description = "Competitor nickname", example = "Smally")
        String nickname,

        @Schema(description = "Competitor type", example = "DWARF")
        CompetitorType competitorType,

        @Schema(description = "Competitor status", example = "ACTIVE")
        CompetitorStatus status
) {
}