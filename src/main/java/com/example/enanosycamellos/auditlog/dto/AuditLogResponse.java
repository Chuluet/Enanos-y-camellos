package com.example.enanosycamellos.auditlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A single audit log entry")
public record AuditLogResponse(
        UUID id,
        String username,
        String action,
        String entityType,
        String entityId,
        LocalDateTime timestamp,
        String description,
        String oldValue,
        String newValue
) {
}