package com.example.enanosycamellos.result.dto;

import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.result.entity.ResultStatus;
import com.example.enanosycamellos.team.dto.TeamSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Full result data. Exactly one of competitor/team is present.")
public record RaceResultResponse(

        UUID id,
        UUID registrationId,
        RaceSummaryResponse race,

        @Schema(description = "Present only when the result belongs to an individual registration")
        CompetitorSummaryResponse competitor,

        @Schema(description = "Present only when the result belongs to a team registration")
        TeamSummaryResponse team,

        Integer startPosition,
        Integer finalPosition,
        Double completionTime,
        Double penaltyTime,
        ResultStatus status,
        String notes,
        String recordedBy,
        LocalDateTime recordedAt
) {
}