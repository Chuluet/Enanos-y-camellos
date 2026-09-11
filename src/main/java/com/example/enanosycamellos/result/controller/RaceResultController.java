package com.example.enanosycamellos.result.controller;

import com.example.enanosycamellos.common.exceptions.ErrorResponse;
import com.example.enanosycamellos.result.dto.RaceResultRequest;
import com.example.enanosycamellos.result.dto.RaceResultResponse;
import com.example.enanosycamellos.result.dto.RaceResultUpdateRequest;
import com.example.enanosycamellos.result.service.RaceResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Results", description = "Race result recording")
public class RaceResultController {

    private final RaceResultService resultService;

    @GetMapping("/api/races/{raceId}/results")
    @Operation(summary = "List results for a race")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List obtained"),
            @ApiResponse(responseCode = "404", description = "Race does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RaceResultResponse>> getResultsByRace(
            @Parameter(description = "Race id") @PathVariable UUID raceId) {

        return ResponseEntity.ok(resultService.getByRace(raceId));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @PostMapping("/api/races/{raceId}/results")
    @Operation(summary = "Record a result for an approved registration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Result recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid data (missing completionTime for FINISHED, etc.)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registration does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Race not IN_PROGRESS, registration not APPROVED, "
                    + "already has a result, or position taken",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceResultResponse> recordResult(
            @Parameter(description = "Race id, kept in the path for a consistent nested URL "
                    + "(the registrationId inside the body is what's actually used)")
            @PathVariable UUID raceId,
            @Valid @RequestBody RaceResultRequest request) {

        RaceResultResponse created = resultService.record(request);
        return ResponseEntity
                .created(URI.create("/api/results/" + created.id()))
                .body(created);
    }

    @GetMapping("/api/results/{id}")
    @Operation(summary = "Find a result by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result found"),
            @ApiResponse(responseCode = "404", description = "Result does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceResultResponse> getById(
            @Parameter(description = "Result id") @PathVariable UUID id) {

        return ResponseEntity.ok(resultService.getById(id));
    }
    
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @PutMapping("/api/results/{id}")
    @Operation(summary = "Fully replace a result's mutable data", description = "Also re-syncs stats consistently.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Result does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Race no longer IN_PROGRESS or position taken",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceResultResponse> updateResult(
            @Parameter(description = "Result id") @PathVariable UUID id,
            @Valid @RequestBody RaceResultUpdateRequest request) {

        return ResponseEntity.ok(resultService.update(id, request));
    }
}