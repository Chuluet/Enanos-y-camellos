package com.example.enanosycamellos.registration.controller;

import com.example.enanosycamellos.common.exceptions.ErrorResponse;
import com.example.enanosycamellos.registration.dto.RaceRegistrationRequest;
import com.example.enanosycamellos.registration.dto.RaceRegistrationResponse;
import com.example.enanosycamellos.registration.dto.RegistrationRejectRequest;
import com.example.enanosycamellos.registration.service.RegistrationService;
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
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Registrations", description = "Race registration management")
public class RegistrationController {

    private final RegistrationService registrationService;

    @GetMapping("/api/races/{raceId}/registrations")
    @Operation(summary = "List registrations for a race")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List obtained"),
            @ApiResponse(responseCode = "404", description = "Race does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RaceRegistrationResponse>> getRegistrationsByRace(
            @Parameter(description = "Race id") @PathVariable UUID raceId) {

        return ResponseEntity.ok(registrationService.getByRace(raceId));
    }

    @PostMapping("/api/races/{raceId}/registrations")
    @Operation(summary = "Register a competitor or a team for a race")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration created"),
            @ApiResponse(responseCode = "400", description = "Invalid data (both/neither participant, type mismatch)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Race, competitor or team does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule violated (closed race, duplicate, ineligible, taken position)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceRegistrationResponse> register(
            @Parameter(description = "Race id") @PathVariable UUID raceId,
            @Valid @RequestBody RaceRegistrationRequest request) {

        RaceRegistrationResponse created = registrationService.register(raceId, request);
        return ResponseEntity
                .created(URI.create("/api/registrations/" + created.id()))
                .body(created);
    }

    @GetMapping("/api/registrations/{id}")
    @Operation(summary = "Find a registration by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Registration does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceRegistrationResponse> getById(
            @Parameter(description = "Registration id") @PathVariable UUID id) {

        return ResponseEntity.ok(registrationService.getById(id));
    }

    @PatchMapping("/api/registrations/{id}/approve")
    @Operation(summary = "Approve a pending registration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration approved"),
            @ApiResponse(responseCode = "404", description = "Registration does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Registration is not PENDING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceRegistrationResponse> approve(
            @Parameter(description = "Registration id") @PathVariable UUID id) {

        return ResponseEntity.ok(registrationService.approve(id));
    }

    @PatchMapping("/api/registrations/{id}/reject")
    @Operation(summary = "Reject a pending registration", description = "Requires a reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration rejected"),
            @ApiResponse(responseCode = "400", description = "Missing reason",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registration does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Registration is not PENDING",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RaceRegistrationResponse> reject(
            @Parameter(description = "Registration id") @PathVariable UUID id,
            @Valid @RequestBody RegistrationRejectRequest request) {

        return ResponseEntity.ok(registrationService.reject(id, request.reason()));
    }

    @DeleteMapping("/api/registrations/{id}")
    @Operation(summary = "Cancel a registration", description = "Sets status to CANCELLED, does not delete the row.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registration cancelled"),
            @ApiResponse(responseCode = "404", description = "Registration does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Already rejected or cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cancel(
            @Parameter(description = "Registration id") @PathVariable UUID id) {

        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}