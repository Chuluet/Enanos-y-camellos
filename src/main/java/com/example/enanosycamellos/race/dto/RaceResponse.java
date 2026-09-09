package com.example.enanosycamellos.race.dto;

import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Full race data")
public record RaceResponse(

        UUID id,
        String name,
        String description,
        LocalDateTime scheduledDateTime,
        String startLocation,
        String finishLocation,
        Integer distanceMeters,
        Integer maxParticipants,
        RaceType raceType,
        RaceStatus status,
        String organizer,
        LocalDateTime registrationDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}