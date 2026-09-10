package com.example.enanosycamellos.registration.dto;

import com.example.enanosycamellos.competitor.dto.CompetitorSummaryResponse;
import com.example.enanosycamellos.race.dto.RaceSummaryResponse;
import com.example.enanosycamellos.registration.entity.RegistrationStatus;
import com.example.enanosycamellos.team.dto.TeamSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Full registration data. Exactly one of competitor/team is present.")
public record RaceRegistrationResponse(

        UUID id,
        RaceSummaryResponse race,

        @Schema(description = "Present only for individual registrations")
        CompetitorSummaryResponse competitor,

        @Schema(description = "Present only for team registrations")
        TeamSummaryResponse team,

        LocalDateTime registrationDate,
        RegistrationStatus status,
        Integer startingPosition,
        String validationNotes,
        String registeredBy
) {
}