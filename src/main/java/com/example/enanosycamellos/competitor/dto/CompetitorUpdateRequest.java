package com.example.enanosycamellos.competitor.dto;

import com.example.enanosycamellos.competitor.entity.CompetitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Data to fully replace a competitor with PUT. Per section 6 of the
 * assignment, PUT means full replacement: every editable field is
 * mandatory, same as on creation — this is NOT a partial patch.
 *
 * <p>status and the statistics (victories/defeats/completedRaces) are
 * intentionally absent: status has its own endpoint
 * (PATCH /competitors/{id}/status) and the statistics are owned by the
 * Results module, never by a client-supplied update.</p>
 */
@Schema(description = "Data to fully update a competitor. Status and statistics are not editable here.")
public record CompetitorUpdateRequest(

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
        String origin
) {

    /** Same rule as on creation: at least one of the two must be present. */
    public boolean isMissingAgeInfo() {
        return dateOfBirth == null && approximateAge == null;
    }
}