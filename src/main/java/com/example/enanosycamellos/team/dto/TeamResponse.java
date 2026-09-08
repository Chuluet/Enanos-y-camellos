package com.example.enanosycamellos.team.dto;

import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.team.entity.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What the API returns for a team: its own data plus its members
 * (competitors), in short version to avoid recursion.
 */
@Schema(description = "Team with its members")
public record TeamResponse(

        @Schema(description = "Team identifier")
        UUID id,

        @Schema(description = "Team name", example = "The Five Exceptions")
        String name,

        @Schema(description = "Team description", example = "Five dwarfs against the odds")
        String description,

        @Schema(description = "Coach or responsible person", example = "Mr. Abandonado")
        String coach,

        @Schema(description = "Team creation date")
        LocalDate creationDate,

        @Schema(description = "Team status", example = "ACTIVE")
        TeamStatus status,

        @Schema(description = "Total victories", example = "3")
        int victories,

        @Schema(description = "Total defeats", example = "1")
        int defeats,

        @Schema(description = "Maximum number of members allowed", example = "5")
        int maxMembers,

        @Schema(description = "Members currently assigned to this team")
        List<CompetitorSummaryResponse> members
) {
}