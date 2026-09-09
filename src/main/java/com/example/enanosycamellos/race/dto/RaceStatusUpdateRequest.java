package com.example.enanosycamellos.race.dto;

import com.example.enanosycamellos.race.entity.RaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New status to transition the race to")
public record RaceStatusUpdateRequest(

        @NotNull(message = "status is mandatory")
        @Schema(description = "Target status", example = "OPEN_FOR_REGISTRATION")
        RaceStatus status
) {
}