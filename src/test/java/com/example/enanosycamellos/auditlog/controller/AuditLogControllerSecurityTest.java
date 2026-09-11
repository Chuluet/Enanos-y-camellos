package com.example.enanosycamellos.auditlog.controller;

import com.example.enanosycamellos.auditlog.service.AuditLogService;
import com.example.enanosycamellos.common.config.SecurityConfig;
import com.example.enanosycamellos.common.config.JacksonConfig;
import com.example.enanosycamellos.common.config.RestAccessDeniedHandler;
import com.example.enanosycamellos.common.config.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JacksonConfig.class})
class AuditLogControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void getAll_shouldReturn200_whenUserIsAdministrator() throws Exception {
        when(auditLogService.getAll(null, null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RACE_ORGANIZER")
    void getAll_shouldReturn403_whenUserIsRaceOrganizer() throws Exception {
        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getAll_shouldReturn403_whenUserIsViewer() throws Exception {
        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isUnauthorized());
    }
}