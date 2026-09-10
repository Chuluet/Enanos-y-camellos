package com.example.enanosycamellos.auditlog.repository;

import com.example.enanosycamellos.auditlog.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IAuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findAllByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    List<AuditLog> findAllByUsernameOrderByTimestampDesc(String username);

    List<AuditLog> findAllByTimestampBetweenOrderByTimestampDesc(LocalDateTime from, LocalDateTime to);
}