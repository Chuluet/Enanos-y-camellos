package com.example.enanosycamellos.team.dto;

import com.example.enanosycamellos.team.entity.TeamStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Fields to partially modify of a team. Those not sent are left unchanged.")
public record TeamPatchRequest(

        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        @Schema(description = "New name", example = "The Five Exceptions II")
        String name,

        @Size(max = 255, message = "description must be at most 255 characters")
        @Schema(description = "New description", example = "Now with a new dwarf")
        String description,

        @Size(min = 2, max = 100, message = "coach must be between 2 and 100 characters")
        @Schema(description = "New coach", example = "Ms. Abandonada")
        String coach,

        @Min(value = 1, message = "maxMembers must be greater than 0")
        @Schema(description = "New maximum number of members", example = "6")
        Integer maxMembers,

        @Schema(description = "New status", example = "SUSPENDED")
        TeamStatus status
) {

    /** True if the request brings no fields: there would be nothing to update. */
    public boolean isEmpty() {
        return name == null
                && description == null
                && coach == null
                && maxMembers == null
                && status == null;
    }
}