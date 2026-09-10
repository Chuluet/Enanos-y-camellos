package com.example.enanosycamellos.standings.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A team's position in the league standings")
public record TeamStandingResponse(
        UUID teamId,
        String name,
        int totalPoints,
        int victories,
        int defeats
) {
}