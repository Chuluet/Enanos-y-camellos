package com.example.enanosycamellos.race.dto;

import com.example.enanosycamellos.race.entity.RaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Schema(description = "Data to create a race")
public record RaceRequest(

        @NotBlank(message = "name is mandatory")
        @Size(min = 2, max = 150, message = "name must be between 2 and 150 characters")
        String name,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @NotNull(message = "scheduledDateTime is mandatory")
        @Future(message = "scheduledDateTime must be in the future")
        LocalDateTime scheduledDateTime,

        @NotBlank(message = "startLocation is mandatory")
        @Size(max = 150, message = "startLocation must be at most 150 characters")
        String startLocation,

        @NotBlank(message = "finishLocation is mandatory")
        @Size(max = 150, message = "finishLocation must be at most 150 characters")
        String finishLocation,

        @NotNull(message = "distanceMeters is mandatory")
        @Positive(message = "distanceMeters must be greater than 0")
        Integer distanceMeters,

        @NotNull(message = "maxParticipants is mandatory")
        @Min(value = 2, message = "maxParticipants must be at least 2")
        Integer maxParticipants,

        @NotNull(message = "raceType is mandatory")
        RaceType raceType,

        @NotBlank(message = "organizer is mandatory")
        @Size(max = 100, message = "organizer must be at most 100 characters")
        String organizer,

        @NotNull(message = "registrationDeadline is mandatory")
        @Future(message = "registrationDeadline must be in the future")
        LocalDateTime registrationDeadline
) {
    @JsonIgnore
    public boolean isDeadlineBeforeStart() {
        return registrationDeadline != null
                && scheduledDateTime != null
                && registrationDeadline.isBefore(scheduledDateTime);
    }
}