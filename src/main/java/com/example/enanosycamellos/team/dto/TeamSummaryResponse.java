package com.example.enanosycamellos.team.dto;

import com.example.enanosycamellos.team.entity.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record TeamSummaryResponse(
        @Schema(description = "Team identifier")
        UUID id,

        @Schema(description = "Team name", example = "Isaac cancele")
        String name,

        @Schema(description = "Team status", example = "ACTIVE")
        TeamStatus status
) {
}
