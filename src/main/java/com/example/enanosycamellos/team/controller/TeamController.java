package com.example.enanosycamellos.team.controller;

import com.example.enanosycamellos.common.exceptions.ErrorResponse;
import com.example.enanosycamellos.team.dto.TeamPatchRequest;
import com.example.enanosycamellos.team.dto.TeamRequest;
import com.example.enanosycamellos.team.dto.TeamResponse;
import com.example.enanosycamellos.team.dto.TeamUpdateRequest;
import com.example.enanosycamellos.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Team endpoints.
 *
 * <p>Same rule as {@code CowController}: no business logic here, no
 * try/catch. Errors are handled by {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Teams", description = "Team CRUD and member management (competitors)")
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @Operation(summary = "List teams", description = "Returns all non-inactive teams with their members.")
    @ApiResponse(responseCode = "200", description = "List obtained")
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        return ResponseEntity.ok(teamService.getTeams());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a team by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team found"),
            @ApiResponse(responseCode = "404", description = "Team does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeamResponse> getTeamById(
            @Parameter(description = "Team id") @PathVariable UUID id) {

        return ResponseEntity.ok(teamService.getById(id));
    }

    @PostMapping
    @Operation(
            summary = "Create a team",
            description = "Optionally assigns initial members via memberIds. "
                    + "Each id must belong to an active competitor with no current team."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Team created"),
            @ApiResponse(responseCode = "400", description = "Invalid data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Some competitor does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "There is already a team with that name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamRequest request) {
        TeamResponse created = teamService.create(request);
        return ResponseEntity
                .created(URI.create("/api/teams/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Fully update a team",
            description = "Replaces every editable field. Members and statistics are not editable here: "
                    + "use the member endpoints for membership changes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Name taken or maxMembers below current member count",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeamResponse> updateTeam(
            @Parameter(description = "Team id") @PathVariable UUID id,
            @Valid @RequestBody TeamUpdateRequest request) {

        return ResponseEntity.ok(teamService.update(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Partially modify a team",
            description = "Only changes the fields that are sent; those not provided are left unchanged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data or empty request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Name taken or maxMembers below current member count",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeamResponse> patchTeam(
            @Parameter(description = "Team id") @PathVariable UUID id,
            @Valid @RequestBody TeamPatchRequest request) {

        return ResponseEntity.ok(teamService.patch(id, request));
    }

    @PostMapping("/{teamId}/members/{competitorId}")
    @Operation(summary = "Add a competitor to a team")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Competitor added"),
            @ApiResponse(responseCode = "400", description = "Competitor is not active",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team or competitor does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Team is full, inactive, or competitor already has a team",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeamResponse> addMember(
            @Parameter(description = "Team id") @PathVariable UUID teamId,
            @Parameter(description = "Competitor id") @PathVariable UUID competitorId) {

        return ResponseEntity.ok(teamService.addMember(teamId, competitorId));
    }

    @DeleteMapping("/{teamId}/members/{competitorId}")
    @Operation(summary = "Remove a competitor from a team")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Competitor removed"),
            @ApiResponse(responseCode = "404", description = "Team, competitor, or membership does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TeamResponse> removeMember(
            @Parameter(description = "Team id") @PathVariable UUID teamId,
            @Parameter(description = "Competitor id") @PathVariable UUID competitorId) {

        return ResponseEntity.ok(teamService.removeMember(teamId, competitorId));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deactivate a team",
            description = "The team is not deleted: status is set to INACTIVE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Team deactivated"),
            @ApiResponse(responseCode = "404", description = "Team does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteTeam(
            @Parameter(description = "Team id") @PathVariable UUID id) {

        teamService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}