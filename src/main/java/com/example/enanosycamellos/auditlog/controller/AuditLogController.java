package com.example.enanosycamellos.auditlog.controller;

import com.example.enanosycamellos.auditlog.dto.AuditLogResponse;
import com.example.enanosycamellos.auditlog.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Read-only trail of sensitive actions. Admin-only once Module 1 (security) lands.")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping
    @Operation(summary = "List audit log entries",
            description = "Optionally filtered by entityType+entityId, by username, or by a date range. "
                    + "Only ADMIN will be allowed to call this once security is wired up.")
    @ApiResponse(responseCode = "200", description = "List obtained")
    public ResponseEntity<List<AuditLogResponse>> getAll(
            @Parameter(description = "Filter by entity type, e.g. 'Race'") @RequestParam(required = false) String entityType,
            @Parameter(description = "Filter by entity id, used together with entityType") @RequestParam(required = false) String entityId,
            @Parameter(description = "Filter by the username who performed the action") @RequestParam(required = false) String username,
            @Parameter(description = "Start of a date range filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End of a date range filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(auditLogService.getAll(entityType, entityId, username, from, to));
    }
}