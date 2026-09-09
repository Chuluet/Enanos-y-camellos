package com.example.enanosycamellos.team.dto;

import com.example.enanosycamellos.team.entity.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Data to fully update a team. Members and statistics are not editable here.")
public record TeamUpdateRequest(

        @NotBlank(message = "name is mandatory")
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        @Schema(description = "Team name (must be unique)", example = "The Five Exceptions")
        String name,

        @Size(max = 255, message = "description must be at most 255 characters")
        @Schema(description = "Team description", example = "Five dwarfs against the odds")
        String description,

        @NotBlank(message = "coach is mandatory")
        @Size(min = 2, max = 100, message = "coach must be between 2 and 100 characters")
        @Schema(description = "Coach or responsible person", example = "Mr. Abandonado")
        String coach,

        @Min(value = 1, message = "maxMembers must be greater than 0")
        @Schema(description = "Maximum number of members allowed in the team", example = "5")
        int maxMembers,

        @NotNull(message = "status is mandatory")
        @Schema(description = "Team status", example = "ACTIVE")
        TeamStatus status
) {
}