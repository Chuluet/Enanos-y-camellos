package com.example.enanosycamellos.auditlog.repository;

import com.example.enanosycamellos.auditlog.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AuditLogRepositoryIntegrationTest {

    @Autowired
    private IAuditLogRepository auditLogRepository;

    @Autowired
    private TestEntityManager em;

    private AuditLog persistEntry(String username, String entityType, String entityId, LocalDateTime timestamp) {
        AuditLog entry = AuditLog.builder()
                .username(username)
                .action("STATUS_CHANGE")
                .entityType(entityType)
                .entityId(entityId)
                .description("test entry")
                .build();
        AuditLog saved = em.persistAndFlush(entry);
        // timestamp se pisa en @PrePersist con LocalDateTime.now(); para probar
        // ordenamiento y rango de fechas de forma determinística, lo ajustamos
        // directamente después de persistir.
        saved.setTimestamp(timestamp);
        return em.persistAndFlush(saved);
    }

    @BeforeEach
    void setUp() {
        persistEntry("mr.abandonado", "Race", "race-1", LocalDateTime.now().minusHours(3));
        persistEntry("organizer", "Race", "race-1", LocalDateTime.now().minusHours(2));
        persistEntry("organizer", "Registration", "reg-1", LocalDateTime.now().minusHours(1));
    }

    @Test
    @DisplayName("findAllByOrderByTimestampDesc returns every entry, newest first")
    void findAllOrderedByTimestamp_returnsAllEntriesNewestFirst() {
        List<AuditLog> result = auditLogRepository.findAllByOrderByTimestampDesc();

        assertEquals(3, result.size());
        assertEquals("Registration", result.get(0).getEntityType()); // most recent
        assertEquals("Race", result.get(2).getEntityType()); // oldest
    }

    @Test
    @DisplayName("findAllByEntityTypeAndEntityIdOrderByTimestampDesc filters by entity")
    void findAllByEntity_returnsOnlyMatchingEntity() {
        List<AuditLog> result =
                auditLogRepository.findAllByEntityTypeAndEntityIdOrderByTimestampDesc("Race", "race-1");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getEntityType().equals("Race") && e.getEntityId().equals("race-1")));
    }

    @Test
    @DisplayName("findAllByUsernameOrderByTimestampDesc filters by username")
    void findAllByUsername_returnsOnlyMatchingUser() {
        List<AuditLog> result = auditLogRepository.findAllByUsernameOrderByTimestampDesc("organizer");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getUsername().equals("organizer")));
    }

    @Test
    @DisplayName("findAllByTimestampBetweenOrderByTimestampDesc filters by date range")
    void findAllByDateRange_returnsOnlyEntriesInRange() {
        LocalDateTime from = LocalDateTime.now().minusHours(2).minusMinutes(30);
        LocalDateTime to = LocalDateTime.now().minusHours(1).minusMinutes(30);

        List<AuditLog> result = auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(from, to);

        assertEquals(1, result.size());
        assertEquals("race-1", result.get(0).getEntityId());
    }
}