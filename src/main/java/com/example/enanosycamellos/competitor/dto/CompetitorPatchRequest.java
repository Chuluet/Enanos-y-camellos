package com.example.enanosycamellos.competitor.dto;

import com.example.enanosycamellos.competitor.entity.CompetitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data to partially modify a competitor with PATCH.
 *
 * <p>Same PATCH semantics as CowUpdateRequest/TeamUpdateRequest: height and
 * weight are {@code Double} (not the primitive {@code double}) precisely to
 * distinguish "they didn't send the field" (null) from "they sent a 0" —
 * with a primitive, a missing weight would arrive as 0.0 and silently
 * overwrite the existing value.</p>
 *
 * <p>status and the statistics are not here: status has its own endpoint
 * (PATCH /competitors/{id}/status), and the statistics belong to the
 * Results module, never a client-supplied update.</p>
 */
@Schema(description = "Fields to modify of a competitor. Those not sent are left unchanged.")
public record CompetitorPatchRequest(

        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        @Schema(description = "New competitor name", example = "Marvin Alberto")
        String name,

        @Size(min = 2, max = 100, message = "nickname must be between 2 and 100 characters")
        @Schema(description = "New competitor nickname (must be unique)", example = "Smally")
        String nickname,

        @Schema(description = "New competitor type", example = "DWARF")
        CompetitorType competitorType,

        @Past(message = "date of birth must be in the past")
        @Schema(description = "New date of birth", example = "1990-01-01")
        LocalDate dateOfBirth,

        @Min(value = 0, message = "approximateAge cannot be negative")
        @Schema(description = "New approximate age", example = "35")
        Integer approximateAge,

        @Positive(message = "height must be greater than 0")
        @Schema(description = "New height in centimeters", example = "120.5")
        Double height,

        @Positive(message = "weight must be greater than 0")
        @Schema(description = "New weight in kilograms", example = "45.0")
        Double weight,

        @Size(min = 2, max = 100, message = "origin must be between 2 and 100 characters")
        @Schema(description = "New country or place of origin", example = "Spain")
        String origin
) {

    /** True if the request brings no fields: there would be nothing to update. */
    public boolean isEmpty() {
        return name == null
                && nickname == null
                && competitorType == null
                && dateOfBirth == null
                && approximateAge == null
                && height == null
                && weight == null
                && origin == null;
    }
}