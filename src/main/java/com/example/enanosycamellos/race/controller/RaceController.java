package com.example.enanosycamellos.race.controller;

import com.example.enanosycamellos.common.exceptions.ErrorResponse;
import com.example.enanosycamellos.race.dto.RaceRequest;
import com.example.enanosycamellos.race.dto.RaceResponse;
import com.example.enanosycamellos.race.dto.RaceStatusUpdateRequest;
import com.example.enanosycamellos.race.dto.RaceUpdateRequest;
import com.example.enanosycamellos.race.entity.RaceStatus;
import com.example.enanosycamellos.race.entity.RaceType;
import com.example.enanosycamellos.race.service.RaceService;
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

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Races", description = "Race CRUD and status lifecycle")
public class RaceController {

    private final RaceService raceService;

    @GetMapping
    @Operation(summary = "List races")
    @ApiResponse(responseCode = "200", description = "List obtained")
    public ResponseEntity<List<RaceResponse>> getAllRaces(
            @Parameter(description = "Filter by status") @RequestParam(required = false) RaceStatus status,
            @Parameter(description = "Filter by race type") @RequestParam(required = false) RaceType type) {

        return ResponseEntity.ok(raceService.getRaces(status, type));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a race by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Race found"),
            @ApiResponse(responseCode = "404", description = "Race does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceResponse> getRaceById(
            @Parameter(description = "Race id") @PathVariable UUID id) {

        return ResponseEntity.ok(raceService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a race", description = "Always starts in DRAFT status.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Race created"),
            @ApiResponse(responseCode = "400", description = "Invalid data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceResponse> createRace(@Valid @RequestBody RaceRequest request) {
        RaceResponse created = raceService.create(request);
        return ResponseEntity
                .created(URI.create("/api/races/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modify a race")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Race updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data or empty request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Race does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Race is already completed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceResponse> updateRace(
            @Parameter(description = "Race id") @PathVariable UUID id,
            @Valid @RequestBody RaceUpdateRequest request) {

        return ResponseEntity.ok(raceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a race's status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "404", description = "Race does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transition not allowed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceResponse> changeStatus(
            @Parameter(description = "Race id") @PathVariable UUID id,
            @Valid @RequestBody RaceStatusUpdateRequest request) {

        return ResponseEntity.ok(raceService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a race", description = "Sets status to CANCELLED, does not delete the row.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Race cancelled"),
            @ApiResponse(responseCode = "404", description = "Race does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cannot be cancelled from its current status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cancelRace(
            @Parameter(description = "Race id") @PathVariable UUID id) {

        raceService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}