package com.example.enanosycamellos.race.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Fields to modify of a race. Those not sent are left unchanged. "
        + "Status is not changed here: use PATCH /api/races/{id}/status instead.")
public record RaceUpdateRequest(

        @Size(min = 2, max = 150, message = "name must be between 2 and 150 characters")
        String name,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @Future(message = "scheduledDateTime must be in the future")
        LocalDateTime scheduledDateTime,

        @Size(max = 150, message = "startLocation must be at most 150 characters")
        String startLocation,

        @Size(max = 150, message = "finishLocation must be at most 150 characters")
        String finishLocation,

        @Positive(message = "distanceMeters must be greater than 0")
        Integer distanceMeters,

        @Min(value = 2, message = "maxParticipants must be at least 2")
        Integer maxParticipants,

        @Future(message = "registrationDeadline must be in the future")
        LocalDateTime registrationDeadline
) {

    public boolean isEmpty() {
        return name == null
                && description == null
                && scheduledDateTime == null
                && startLocation == null
                && finishLocation == null
                && distanceMeters == null
                && maxParticipants == null
                && registrationDeadline == null;
    }
}