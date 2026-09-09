package com.example.enanosycamellos.competitor.dto;

import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Body for PATCH /api/competitors/{id}/status. */
@Schema(description = "New status for a competitor")
public record CompetitorStatusUpdateRequest(

        @NotNull(message = "status is mandatory")
        @Schema(description = "New competitor status", example = "INJURED")
        CompetitorStatus status
) {
}