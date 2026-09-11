package com.example.enanosycamellos.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;

@Schema(description = "Data to register a competitor or a team for a race. "
        + "Exactly one of competitorId/teamId must be sent, never both.")
public record RaceRegistrationRequest(

        @Schema(description = "Competitor id, for individual registrations")
        UUID competitorId,

        @Schema(description = "Team id, for team registrations")
        UUID teamId,

        @Positive(message = "startingPosition must be greater than 0")
        @Schema(description = "Optional starting position/lane, assigned at registration time")
        Integer startingPosition,

        @NotBlank(message = "registeredBy is mandatory")
        @Size(max = 100, message = "registeredBy must be at most 100 characters")
        @Schema(description = "Username of whoever performs the registration", example = "mr.abandonado")
        String registeredBy
) {

    /** Exactly one of the two ids must be present. */
    public boolean hasExactlyOneParticipant() {
        return (competitorId != null) ^ (teamId != null);
    }
    
    @JsonIgnore
    public boolean isIndividual() {
        return competitorId != null;
    }
}