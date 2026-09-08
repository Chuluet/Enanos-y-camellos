package com.example.enanosycamellos.team.dto;

import com.example.enanosycamellos.team.entity.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Data to create a team")
public record TeamRequest(

        @NotBlank(message = "name is mandatory")
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        @Schema(description = "Team name (must be unique)", example = "Isaac cancele")
        String name,

        @Size(max = 255, message = "description must be at most 255 characters")
        @Schema(description = "Team description", example = "Micro team, each member smaller than the last")
        String description,

        @NotBlank(message = "coach is mandatory")
        @Size(min = 2, max = 100, message = "coach must be between 2 and 100 characters")
        @Schema(description = "Coach or responsible person", example = "Mr. Pardo")
        String coach,

        @Min(value = 1, message = "maxMembers must be greater than 0")
        @Schema(description = "Maximum number of members allowed in the team", example = "5")
        int maxMembers,

        @Schema(description = "Initial status of the team. Defaults to ACTIVE if not sent.",
                example = "ACTIVE")
        TeamStatus status,

        @Schema(description = "Ids of the competitors to assign to this team at creation. Optional.")
        List<UUID> memberIds
) {

    public List<UUID> memberIdsOrEmpty() {
        return memberIds == null ? List.of() : memberIds;
    }

    public TeamStatus statusOrDefault() {
        return status == null ? TeamStatus.ACTIVE : status;
    }
}