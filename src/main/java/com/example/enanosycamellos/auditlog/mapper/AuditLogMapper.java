package com.example.enanosycamellos.auditlog.mapper;

import com.example.enanosycamellos.auditlog.dto.AuditLogResponse;
import com.example.enanosycamellos.auditlog.entity.AuditLog;

import java.util.List;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog entry) {
        if (entry == null) return null;
        return new AuditLogResponse(
                entry.getId(),
                entry.getUsername(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getTimestamp(),
                entry.getDescription(),
                entry.getOldValue(),
                entry.getNewValue()
        );
    }

    public static List<AuditLogResponse> toResponseList(List<AuditLog> entries) {
        if (entries == null) return List.of();
        return entries.stream().map(AuditLogMapper::toResponse).toList();
    }
}