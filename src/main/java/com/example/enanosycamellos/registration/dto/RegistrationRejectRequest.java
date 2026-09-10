package com.example.enanosycamellos.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Reason for rejecting a registration")
public record RegistrationRejectRequest(

        @NotBlank(message = "reason is mandatory")
        @Size(max = 500, message = "reason must be at most 500 characters")
        @Schema(description = "Clear reason for the rejection", example = "Competitor is currently SUSPENDED")
        String reason
) {
}