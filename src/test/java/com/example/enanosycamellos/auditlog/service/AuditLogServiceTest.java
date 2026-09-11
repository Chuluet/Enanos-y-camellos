package com.example.enanosycamellos.auditlog.service;

import com.example.enanosycamellos.auditlog.entity.AuditLog;
import com.example.enanosycamellos.auditlog.repository.IAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private IAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;


    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ============================== log() ==============================

    @Test
    void log_shouldUseSystemAsUsername_whenNoAuthenticationIsPresent() {
        SecurityContextHolder.clearContext();
        IAuditLogRepository repository = mock(IAuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository, new ObjectMapper());

        service.log("CREATE", "Race", "some-id", "Race created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("system");
    }

    @Test
    void log_shouldUseSystemAsUsername_whenAuthenticationIsAnonymous() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        IAuditLogRepository repository = mock(IAuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository, new ObjectMapper());

        service.log("CREATE", "Race", "some-id", "Race created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("system");
    }

    @Test
    void log_shouldUseRealUsername_whenAuthenticated() {
        Authentication authenticated = new TestingAuthenticationToken("mr.abandonado", "n/a", "ROLE_ADMIN");
        authenticated.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authenticated);

        IAuditLogRepository repository = mock(IAuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository, new ObjectMapper());

        service.log("CREATE", "Race", "some-id", "Race created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("mr.abandonado");
    }

    @Test
    void log_shouldStoreAllBasicFields() {
        IAuditLogRepository repository = mock(IAuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository, new ObjectMapper());

        service.log("STATUS_CHANGE", "Race", "race-123", "Race status changed");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog entry = captor.getValue();
        assertThat(entry.getAction()).isEqualTo("STATUS_CHANGE");
        assertThat(entry.getEntityType()).isEqualTo("Race");
        assertThat(entry.getEntityId()).isEqualTo("race-123");
        assertThat(entry.getDescription()).isEqualTo("Race status changed");
    }

    @Test
    void log_shouldSerializeOldAndNewValuesAsJson() {
        IAuditLogRepository repository = mock(IAuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository, new ObjectMapper());

        service.log("STATUS_CHANGE", "Race", "race-123", "Status changed", "DRAFT", "OPEN_FOR_REGISTRATION");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOldValue()).contains("DRAFT");
        assertThat(captor.getValue().getNewValue()).contains("OPEN_FOR_REGISTRATION");
    }

    @Test
    void log_shouldLeaveOldAndNewValuesNull_whenNotProvided() {
        IAuditLogRepository repository = mock(IAuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository, new ObjectMapper());

        service.log("CREATE", "Race", "race-123", "Race created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOldValue()).isNull();
        assertThat(captor.getValue().getNewValue()).isNull();
    }

    // ============================== getAll() ==============================

    @Test
    void getAll_shouldFilterByEntityTypeAndEntityId_whenBothAreGiven() {
        when(auditLogRepository.findAllByEntityTypeAndEntityIdOrderByTimestampDesc("Race", "race-123"))
                .thenReturn(List.of());

        auditLogService.getAll("Race", "race-123", null, null, null);

        verify(auditLogRepository).findAllByEntityTypeAndEntityIdOrderByTimestampDesc("Race", "race-123");
        verify(auditLogRepository, never()).findAllByOrderByTimestampDesc();
    }

    @Test
    void getAll_shouldFilterByUsername_whenEntityFilterIsAbsent() {
        when(auditLogRepository.findAllByUsernameOrderByTimestampDesc("mr.abandonado"))
                .thenReturn(List.of());

        auditLogService.getAll(null, null, "mr.abandonado", null, null);

        verify(auditLogRepository).findAllByUsernameOrderByTimestampDesc("mr.abandonado");
    }

    @Test
    void getAll_shouldFilterByDateRange_whenNoOtherFilterIsGiven() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(from, to))
                .thenReturn(List.of());

        auditLogService.getAll(null, null, null, from, to);

        verify(auditLogRepository).findAllByTimestampBetweenOrderByTimestampDesc(from, to);
    }

    @Test
    void getAll_shouldReturnEverything_whenNoFiltersAreGiven() {
        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(List.of());

        auditLogService.getAll(null, null, null, null, null);

        verify(auditLogRepository).findAllByOrderByTimestampDesc();
    }
}