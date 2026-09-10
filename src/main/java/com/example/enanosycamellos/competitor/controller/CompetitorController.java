package com.example.enanosycamellos.competitor.controller;

import com.example.enanosycamellos.common.exceptions.ErrorResponse;
import com.example.enanosycamellos.competitor.dto.*;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.competitor.service.CompetitorService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Competitor endpoints. Same rule as CowController/TeamController: no
 * business logic here, no try/catch. Errors are handled by
 * {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/competitors")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Competitors", description = "Competitor CRUD, filtering, pagination and status management")
public class CompetitorController {

    private final CompetitorService competitorService;

    @GetMapping
    @Operation(
            summary = "List competitors",
            description = "Supports filtering by status and/or competitorType, plus pagination and sorting "
                    + "(e.g. ?page=0&size=10&sort=name,asc)."
    )
    @ApiResponse(responseCode = "200", description = "Page obtained")
    public ResponseEntity<Page<CompetitorResponse>> getAllCompetitors(
            @Parameter(description = "Filter by status") @RequestParam(required = false) CompetitorStatus status,
            @Parameter(description = "Filter by competitor type") @RequestParam(required = false) CompetitorType competitorType,
            Pageable pageable) {

        return ResponseEntity.ok(competitorService.getCompetitors(status, competitorType, pageable));
    }
    @GetMapping("/all")
    @Operation(
            summary = "List competitors",
            description = "Returns all competitors."
    )
    @ApiResponse(responseCode = "200", description = "List obtained")
    public ResponseEntity<List<CompetitorResponse>> getAllCompetitors() {
        return ResponseEntity.ok(competitorService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a competitor by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Competitor found"),
            @ApiResponse(responseCode = "404", description = "Competitor does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CompetitorResponse> getCompetitorById(
            @Parameter(description = "Competitor id") @PathVariable UUID id) {

        return ResponseEntity.ok(competitorService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a competitor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Competitor created"),
            @ApiResponse(responseCode = "400", description = "Invalid data, or missing both dateOfBirth and approximateAge",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "There is already a competitor with that nickname",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CompetitorResponse> createCompetitor(@Valid @RequestBody CompetitorRequest request) {
        CompetitorResponse created = competitorService.create(request);
        return ResponseEntity
                .created(URI.create("/api/competitors/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Fully update a competitor",
            description = "Replaces every editable field. Status and statistics are not editable here: "
                    + "use PATCH /{id}/status for status changes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Competitor updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data, or missing both dateOfBirth and approximateAge",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Competitor does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "There is already another competitor with that nickname",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CompetitorResponse> updateCompetitor(
            @Parameter(description = "Competitor id") @PathVariable UUID id,
            @Valid @RequestBody CompetitorUpdateRequest request) {

        return ResponseEntity.ok(competitorService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a competitor's status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "404", description = "Competitor does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Competitor already has that status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CompetitorResponse> changeStatus(
            @Parameter(description = "Competitor id") @PathVariable UUID id,
            @Valid @RequestBody CompetitorStatusUpdateRequest request) {

        return ResponseEntity.ok(competitorService.changeStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Retire a competitor",
            description = "Not a physical delete: status is set to RETIRED, preserving race history."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Competitor retired"),
            @ApiResponse(responseCode = "404", description = "Competitor does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteCompetitor(
            @Parameter(description = "Competitor id") @PathVariable UUID id) {

        competitorService.retire(id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}")
    @Operation(
            summary = "Partially modify a competitor",
            description = "Only changes the fields that are sent; those not provided are left unchanged. "
                    + "For status changes use PATCH /{id}/status instead."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Competitor updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data or empty request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Competitor does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "There is already another competitor with that nickname",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CompetitorResponse> patchCompetitor(
            @Parameter(description = "Competitor id") @PathVariable UUID id,
            @Valid @RequestBody CompetitorPatchRequest request) {

        return ResponseEntity.ok(competitorService.patch(id, request));
    }
}