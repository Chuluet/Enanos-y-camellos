package com.example.enanosycamellos.auditlog.mapper;

import com.example.enanosycamellos.auditlog.dto.AuditLogResponse;
import com.example.enanosycamellos.auditlog.entity.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogMapperTest {

    private AuditLog buildEntry() {
        return AuditLog.builder()
                .id(UUID.randomUUID())
                .username("mr.abandonado")
                .action("STATUS_CHANGE")
                .entityType("Race")
                .entityId("race-123")
                .timestamp(LocalDateTime.now())
                .description("Race status changed")
                .oldValue("DRAFT")
                .newValue("OPEN_FOR_REGISTRATION")
                .build();
    }

    @Test
    @DisplayName("toResponse maps every field")
    void toResponse_mapsFields() {
        AuditLog entry = buildEntry();

        AuditLogResponse response = AuditLogMapper.toResponse(entry);

        assertEquals(entry.getId(), response.id());
        assertEquals(entry.getUsername(), response.username());
        assertEquals(entry.getAction(), response.action());
        assertEquals(entry.getEntityType(), response.entityType());
        assertEquals(entry.getEntityId(), response.entityId());
        assertEquals(entry.getTimestamp(), response.timestamp());
        assertEquals(entry.getDescription(), response.description());
        assertEquals(entry.getOldValue(), response.oldValue());
        assertEquals(entry.getNewValue(), response.newValue());
    }

    @Test
    @DisplayName("toResponse with null returns null")
    void toResponse_withNull_returnsNull() {
        assertNull(AuditLogMapper.toResponse(null));
    }

    @Test
    @DisplayName("toResponseList maps every element in order")
    void toResponseList_mapsAllElements() {
        AuditLog entry1 = buildEntry();
        AuditLog entry2 = AuditLog.builder()
                .id(UUID.randomUUID()).username("system").action("CREATE")
                .entityType("Registration").entityId("reg-456")
                .timestamp(LocalDateTime.now()).build();

        List<AuditLogResponse> result = AuditLogMapper.toResponseList(List.of(entry1, entry2));

        assertEquals(2, result.size());
        assertEquals(entry1.getId(), result.get(0).id());
        assertEquals(entry2.getId(), result.get(1).id());
    }

    @Test
    @DisplayName("toResponseList with null returns an empty list")
    void toResponseList_withNull_returnsEmptyList() {
        assertTrue(AuditLogMapper.toResponseList(null).isEmpty());
    }
}