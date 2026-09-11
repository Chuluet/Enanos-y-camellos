package com.example.enanosycamellos.auditlog.controller;

import com.example.enanosycamellos.auditlog.dto.AuditLogResponse;
import com.example.enanosycamellos.auditlog.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test: solo la capa web, con AuditLogService mockeado. Seguridad
 * desactivada a propósito, igual que en los demás *ControllerTest — el
 * @PreAuthorize real está comentado en el controller hasta que exista el
 * Módulo 1, así que no hay nada que este test deba simular todavía.
 */
@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    private AuditLogResponse sampleEntry() {
        return new AuditLogResponse(
                UUID.randomUUID(), "mr.abandonado", "STATUS_CHANGE", "Race", "race-123",
                LocalDateTime.now(), "Race status changed", "DRAFT", "OPEN_FOR_REGISTRATION");
    }

    @Test
    void getAll_shouldReturn200_withList() throws Exception {
        when(auditLogService.getAll(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleEntry()));

        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("STATUS_CHANGE"));
    }

    @Test
    void getAll_shouldPassEntityFilters_whenQueryParamsAreGiven() throws Exception {
        when(auditLogService.getAll(eq("Race"), eq("race-123"), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/audit-log")
                        .param("entityType", "Race")
                        .param("entityId", "race-123"))
                .andExpect(status().isOk());

        verify(auditLogService).getAll(eq("Race"), eq("race-123"), isNull(), isNull(), isNull());
    }

    @Test
    void getAll_shouldPassUsernameFilter_whenGiven() throws Exception {
        when(auditLogService.getAll(isNull(), isNull(), eq("mr.abandonado"), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/audit-log").param("username", "mr.abandonado"))
                .andExpect(status().isOk());

        verify(auditLogService).getAll(isNull(), isNull(), eq("mr.abandonado"), isNull(), isNull());
    }
}