package com.example.enanosycamellos.competitor.dto;

import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.team.dto.TeamSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * What the API returns for a competitor: its own data plus its team
 * (in short version, to avoid recursion: competitor -> team -> members ->
 * team -> ...).
 */
@Schema(description = "Competitor with its team")
public record CompetitorResponse(

        @Schema(description = "Competitor identifier")
        UUID id,

        @Schema(description = "Competitor name", example = "Marvin Alberto")
        String name,

        @Schema(description = "Competitor nickname", example = "Smally")
        String nickname,

        @Schema(description = "Competitor type", example = "DWARF")
        CompetitorType competitorType,

        @Schema(description = "Date of birth, if known", example = "1990-01-01")
        LocalDate dateOfBirth,

        @Schema(description = "Approximate age, used when the exact date of birth isn't known", example = "35")
        Integer approximateAge,

        @Schema(description = "Height in centimeters", example = "120.5")
        double height,

        @Schema(description = "Weight in kilograms", example = "45.0")
        double weight,

        @Schema(description = "Country or place of origin", example = "Spain")
        String origin,

        @Schema(description = "Competitor status", example = "ACTIVE")
        CompetitorStatus status,

        @Schema(description = "Registration date")
        LocalDate registrationDate,

        @Schema(description = "Total victories", example = "5")
        int victories,

        @Schema(description = "Total defeats", example = "2")
        int defeats,

        @Schema(description = "Total completed races", example = "10")
        int completedRaces,

        @Schema(description = "Team this competitor belongs to, or null if it has none")
        TeamSummaryResponse team
) {
}