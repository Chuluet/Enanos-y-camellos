package com.example.enanosycamellos.result.dto;

import com.example.enanosycamellos.result.entity.ResultStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Full replacement of a result's mutable data. "
        + "The registration it belongs to cannot be changed.")
public record RaceResultUpdateRequest(

        @Positive(message = "startPosition must be greater than 0")
        Integer startPosition,

        @Positive(message = "finalPosition must be greater than 0")
        Integer finalPosition,

        @Positive(message = "completionTime must be greater than 0")
        Double completionTime,

        @PositiveOrZero(message = "penaltyTime cannot be negative")
        Double penaltyTime,

        @NotNull(message = "status is mandatory")
        ResultStatus status,

        @Size(max = 500, message = "notes must be at most 500 characters")
        String notes,

        @NotBlank(message = "recordedBy is mandatory")
        @Size(max = 100, message = "recordedBy must be at most 100 characters")
        String recordedBy
) {
    @JsonIgnore
    public boolean isCompletionTimeConsistentWithStatus() {
        return status != ResultStatus.FINISHED || (completionTime != null && completionTime > 0);
    }
    @JsonIgnore
    public boolean isFinalPositionConsistentWithStatus() {
        return status == ResultStatus.FINISHED || finalPosition == null;
    }
}