package com.example.enanosycamellos.auditlog.service;

import com.example.enanosycamellos.auditlog.dto.AuditLogResponse;
import com.example.enanosycamellos.auditlog.entity.AuditLog;
import com.example.enanosycamellos.auditlog.mapper.AuditLogMapper;
import com.example.enanosycamellos.auditlog.repository.IAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes and reads audit trail entries. Other services (RaceService,
 * RegistrationService, RaceResultService, and eventually Team/Competitor)
 * call {@link #log} whenever they perform a sensitive action — this class
 * never gets called directly from a controller for writing, only for
 * reading the trail back.
 *
 * <p>{@code currentUsername()} falls back to {@code "system"} because the
 * security module (Module 1) isn't wired up yet: once it is, an
 * authenticated {@link Authentication} will be present in the
 * SecurityContext and this starts returning the real username with no
 * changes needed anywhere else.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final IAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // ============================== Write ===============================

    public void log(String action, String entityType, String entityId, String description) {
        log(action, entityType, entityId, description, null, null);
    }

    public void log(String action, String entityType, String entityId, String description,
                     Object oldValue, Object newValue) {
        AuditLog entry = AuditLog.builder()
                .username(currentUsername())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .oldValue(serialize(oldValue))
                .newValue(serialize(newValue))
                .build();

        auditLogRepository.save(entry);
        log.debug("Audit logged: user={} action={} entityType={} entityId={}",
                entry.getUsername(), action, entityType, entityId);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticatedUser = auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        return isAuthenticatedUser ? auth.getName() : "system";
    }

    private String serialize(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize audit log value, storing as null: {}", e.getMessage());
            return null;
        }
    }

    // ============================== Read ===============================

    /**
     * Simple filter priority (not every combination, to keep this readable):
     * entityType+entityId first, then username, then a date range, and
     * finally everything if nothing was given.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAll(String entityType, String entityId, String username,
                                          LocalDateTime from, LocalDateTime to) {
        List<AuditLog> entries;

        if (entityType != null && entityId != null) {
            entries = auditLogRepository.findAllByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
        } else if (username != null) {
            entries = auditLogRepository.findAllByUsernameOrderByTimestampDesc(username);
        } else if (from != null && to != null) {
            entries = auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(from, to);
        } else {
            entries = auditLogRepository.findAllByOrderByTimestampDesc();
        }

        return AuditLogMapper.toResponseList(entries);
    }
}