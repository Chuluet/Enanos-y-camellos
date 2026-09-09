package com.example.enanosycamellos.race.dto;

import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Short race data, used when embedded in other resources")
public record RaceSummaryResponse(
        UUID id,
        String name,
        RaceType raceType,
        RaceStatus status,
        LocalDateTime scheduledDateTime
) {
}