package com.example.enanosycamellos.competitor.dto;

import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Data to create a competitor.
 *
 * <p>victories/defeats/completedRaces are NOT here on purpose: those are
 * statistics the Results module (Module 6) maintains, not something a client
 * sets at creation — same reasoning already applied to TeamRequest. A new
 * competitor always starts at 0/0/0 in the entity.</p>
 */
@Schema(description = "Data to create a competitor")
public record CompetitorRequest(

        @NotBlank(message = "name is mandatory")
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        @Schema(description = "Competitor name", example = "Marvin Alberto")
        String name,

        @NotBlank(message = "nickname is mandatory")
        @Size(min = 2, max = 100, message = "nickname must be between 2 and 100 characters")
        @Schema(description = "Competitor nickname (must be unique)", example = "Smally")
        String nickname,

        @NotNull(message = "competitorType is mandatory")
        @Schema(description = "Competitor type", example = "DWARF")
        CompetitorType competitorType,

        @Past(message = "date of birth must be in the past")
        @Schema(description = "Date of birth. Provide this or approximateAge.", example = "1990-01-01")
        LocalDate dateOfBirth,

        @Min(value = 0, message = "approximateAge cannot be negative")
        @Schema(description = "Approximate age, used when the exact date of birth isn't known.", example = "35")
        Integer approximateAge,

        @Positive(message = "height must be greater than 0")
        @Schema(description = "Height in centimeters", example = "120.5")
        double height,

        @Positive(message = "weight must be greater than 0")
        @Schema(description = "Weight in kilograms", example = "45.0")
        double weight,

        @NotBlank(message = "origin is mandatory")
        @Size(min = 2, max = 100, message = "origin must be between 2 and 100 characters")
        @Schema(description = "Country or place of origin", example = "Spain")
        String origin,

        @Schema(description = "Status of the competitor. Defaults to ACTIVE if not sent.", example = "ACTIVE")
        CompetitorStatus status
) {

    public CompetitorStatus statusOrDefault() {
        return status == null ? CompetitorStatus.ACTIVE : status;
    }

    /** True if neither dateOfBirth nor approximateAge was sent — the request needs at least one. */
    public boolean isMissingAgeInfo() {
        return dateOfBirth == null && approximateAge == null;
    }
}