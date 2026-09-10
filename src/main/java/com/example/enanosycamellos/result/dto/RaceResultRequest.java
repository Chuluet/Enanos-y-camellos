package com.example.enanosycamellos.result.dto;

import com.example.enanosycamellos.result.entity.ResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Schema(description = "Data to record a race result for an approved registration")
public record RaceResultRequest(

        @NotNull(message = "registrationId is mandatory")
        @Schema(description = "Id of the (approved) registration this result belongs to")
        UUID registrationId,

        @Positive(message = "startPosition must be greater than 0")
        @Schema(description = "Starting position/lane, defaults to the registration's if not sent")
        Integer startPosition,

        @Positive(message = "finalPosition must be greater than 0")
        @Schema(description = "Final position, only meaningful when status is FINISHED")
        Integer finalPosition,

        @Positive(message = "completionTime must be greater than 0")
        @Schema(description = "Completion time, required when status is FINISHED")
        Double completionTime,

        @PositiveOrZero(message = "penaltyTime cannot be negative")
        @Schema(description = "Penalty time added to completionTime, defaults to 0")
        Double penaltyTime,

        @NotNull(message = "status is mandatory")
        @Schema(description = "Result status", example = "FINISHED")
        ResultStatus status,

        @Size(max = 500, message = "notes must be at most 500 characters")
        String notes,

        @NotBlank(message = "recordedBy is mandatory")
        @Size(max = 100, message = "recordedBy must be at most 100 characters")
        @Schema(description = "Username of whoever records the result", example = "mr.abandonado")
        String recordedBy
) {

    /** "Completion time must be positive" only really applies once someone finished. */
    public boolean isCompletionTimeConsistentWithStatus() {
        return status != ResultStatus.FINISHED || (completionTime != null && completionTime > 0);
    }

    /** A disqualified/DNF/DNS participant never gets a final position. */
    public boolean isFinalPositionConsistentWithStatus() {
        return status == ResultStatus.FINISHED || finalPosition == null;
    }
}