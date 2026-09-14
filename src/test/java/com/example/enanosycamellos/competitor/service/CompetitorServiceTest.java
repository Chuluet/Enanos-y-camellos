package com.example.enanosycamellos.competitor.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.dto.CompetitorPatchRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorResponse;
import com.example.enanosycamellos.competitor.dto.CompetitorStatusUpdateRequest;
import com.example.enanosycamellos.competitor.dto.CompetitorUpdateRequest;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.auditlog.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Competitor service unit tests. The repository is mocked to isolate the
 * business logic, same approach as {@code TeamServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class CompetitorServiceTest {

    @Mock
    private ICompetitorRepository competitorRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CompetitorService competitorService;

    // ============================== getCompetitors ===============================

    @Nested
    @DisplayName("getCompetitors")
    class GetCompetitors {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("with status and type, uses the combined filter query")
        void withStatusAndType_usesCombinedQuery() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            Page<Competitor> page = new PageImpl<>(List.of(competitor));
            when(competitorRepository.findAllByStatusAndCompetitorType(CompetitorStatus.ACTIVE, CompetitorType.DWARF, pageable))
                    .thenReturn(page);

            Page<CompetitorResponse> result =
                    competitorService.getCompetitors(CompetitorStatus.ACTIVE, CompetitorType.DWARF, pageable);

            assertEquals(1, result.getTotalElements());
            verify(competitorRepository, never()).findAllByStatusNot(any(), any());
        }

        @Test
        @DisplayName("with only status, uses the exact-status query (includes RETIRED if asked)")
        void withOnlyStatus_usesExactStatusQuery() {
            Competitor retired = buildCompetitor("OT", CompetitorStatus.RETIRED, CompetitorType.DWARF, 3);
            Page<Competitor> page = new PageImpl<>(List.of(retired));
            when(competitorRepository.findAllByStatus(CompetitorStatus.RETIRED, pageable)).thenReturn(page);

            Page<CompetitorResponse> result = competitorService.getCompetitors(CompetitorStatus.RETIRED, null, pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals(CompetitorStatus.RETIRED, result.getContent().getFirst().status());
        }

        @Test
        @DisplayName("with only type, excludes retired competitors by default")
        void withOnlyType_excludesRetiredByDefault() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            Page<Competitor> page = new PageImpl<>(List.of(competitor));
            when(competitorRepository.findAllByCompetitorTypeAndStatusNot(CompetitorType.DWARF, CompetitorStatus.RETIRED, pageable))
                    .thenReturn(page);

            Page<CompetitorResponse> result = competitorService.getCompetitors(null, CompetitorType.DWARF, pageable);

            assertEquals(1, result.getTotalElements());
            verify(competitorRepository, never()).findAllByCompetitorType(any(), any());
        }

        @Test
        @DisplayName("with no filter, excludes retired competitors by default")
        void withNoFilter_excludesRetiredByDefault() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            Page<Competitor> page = new PageImpl<>(List.of(competitor));
            when(competitorRepository.findAllByStatusNot(CompetitorStatus.RETIRED, pageable)).thenReturn(page);

            Page<CompetitorResponse> result = competitorService.getCompetitors(null, null, pageable);

            assertEquals(1, result.getTotalElements());
            verify(competitorRepository, never()).findAll(pageable);
        }
    }

    // ============================== getById ===============================

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns the competitor if it exists")
        void returnsCompetitor() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));

            CompetitorResponse result = competitorService.getById(competitor.getId());

            assertEquals("JD", result.nickname());
        }

        @Test
        @DisplayName("throws 404 if it does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(competitorRepository.findWithTeamById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> competitorService.getById(id));
        }
    }

    // ============================== create ===============================

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates the competitor when valid")
        void createsWhenValid() {
            CompetitorRequest request = new CompetitorRequest(
                    "John D", "JD", CompetitorType.DWARF,
                    LocalDate.of(1989, 6, 15), null, 120.0, 50.0, "USA", CompetitorStatus.ACTIVE);

            when(competitorRepository.existsByNicknameIgnoreCase("JD")).thenReturn(false);
            when(competitorRepository.save(any(Competitor.class))).thenAnswer(i -> {
                Competitor c = i.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            CompetitorResponse result = competitorService.create(request);

            assertNotNull(result);
            assertEquals("JD", result.nickname());
            verify(competitorRepository).save(any(Competitor.class));
        }

        @Test
        @DisplayName("throws 409 if the nickname is already taken")
        void duplicateNickname_throws409() {
            CompetitorRequest request = new CompetitorRequest(
                    "John D", "JD", CompetitorType.DWARF,
                    LocalDate.of(1989, 6, 15), null, 120.0, 50.0, "USA", CompetitorStatus.ACTIVE);
            when(competitorRepository.existsByNicknameIgnoreCase("JD")).thenReturn(true);

            assertThrows(ConflictException.class, () -> competitorService.create(request));
            verify(competitorRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 400 if both dateOfBirth and approximateAge are missing")
        void missingAgeInfo_throws400() {
            CompetitorRequest request = new CompetitorRequest(
                    "John D", "JD", CompetitorType.DWARF,
                    null, null, 120.0, 50.0, "USA", CompetitorStatus.ACTIVE);
            when(competitorRepository.existsByNicknameIgnoreCase("JD")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> competitorService.create(request));
            verify(competitorRepository, never()).save(any());
        }
    }

    // ============================== update (PUT) ===============================

    @Nested
    @DisplayName("update (PUT)")
    class Update {

        @Test
        @DisplayName("replaces every editable field")
        void replacesAllFields() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.existsByNicknameIgnoreCaseAndIdNot("JD2", competitor.getId())).thenReturn(false);
            when(competitorRepository.save(any(Competitor.class))).thenAnswer(i -> i.getArgument(0));

            CompetitorUpdateRequest request = new CompetitorUpdateRequest(
                    "John D II", "JD2", CompetitorType.DWARF,
                    LocalDate.of(1989, 6, 15), null, 121.0, 51.0, "Canada");

            CompetitorResponse result = competitorService.update(competitor.getId(), request);

            assertEquals("John D II", result.name());
            assertEquals("JD2", result.nickname());
            assertEquals("Canada", result.origin());
        }

        @Test
        @DisplayName("throws 404 if the competitor does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(competitorRepository.findWithTeamById(id)).thenReturn(Optional.empty());

            CompetitorUpdateRequest request = new CompetitorUpdateRequest(
                    "John D", "JD", CompetitorType.DWARF,
                    LocalDate.of(1989, 6, 15), null, 120.0, 50.0, "USA");

            assertThrows(ResourceNotFoundException.class, () -> competitorService.update(id, request));
        }

        @Test
        @DisplayName("throws 409 if the new nickname is already taken by someone else")
        void duplicateNickname_throws409() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.existsByNicknameIgnoreCaseAndIdNot("Taken", competitor.getId())).thenReturn(true);

            CompetitorUpdateRequest request = new CompetitorUpdateRequest(
                    "John D", "Taken", CompetitorType.DWARF,
                    LocalDate.of(1989, 6, 15), null, 120.0, 50.0, "USA");

            assertThrows(ConflictException.class, () -> competitorService.update(competitor.getId(), request));
        }

        @Test
        @DisplayName("throws 400 if both dateOfBirth and approximateAge are missing")
        void missingAgeInfo_throws400() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.existsByNicknameIgnoreCaseAndIdNot("JD", competitor.getId())).thenReturn(false);

            CompetitorUpdateRequest request = new CompetitorUpdateRequest(
                    "John D", "JD", CompetitorType.DWARF,
                    null, null, 120.0, 50.0, "USA");

            assertThrows(BadRequestException.class, () -> competitorService.update(competitor.getId(), request));
        }
    }

    // ============================== changeStatus ===============================

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("changes to a different, valid status")
        void changesStatus() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.save(competitor)).thenReturn(competitor);

            CompetitorResponse result = competitorService.changeStatus(
                    competitor.getId(), new CompetitorStatusUpdateRequest(CompetitorStatus.INJURED));

            assertEquals(CompetitorStatus.INJURED, result.status());
        }

        @Test
        @DisplayName("throws 409 if the competitor is already retired (terminal state)")
        void retired_throws409() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.RETIRED, CompetitorType.DWARF, 3);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));

            assertThrows(ConflictException.class, () -> competitorService.changeStatus(
                    competitor.getId(), new CompetitorStatusUpdateRequest(CompetitorStatus.ACTIVE)));
        }

        @Test
        @DisplayName("throws 409 if the competitor already has that exact status")
        void sameStatus_throws409() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));

            assertThrows(ConflictException.class, () -> competitorService.changeStatus(
                    competitor.getId(), new CompetitorStatusUpdateRequest(CompetitorStatus.ACTIVE)));
        }

        @Test
        @DisplayName("throws 404 if the competitor does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(competitorRepository.findWithTeamById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> competitorService.changeStatus(
                    id, new CompetitorStatusUpdateRequest(CompetitorStatus.INJURED)));
        }
    }

    // ============================== retire (logical delete) ===============================

    @Nested
    @DisplayName("retire")
    class Retire {

        @Test
        @DisplayName("sets status to RETIRED, regardless of race history")
        void setsRetired() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 5);
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));

            competitorService.retire(competitor.getId());

            assertEquals(CompetitorStatus.RETIRED, competitor.getStatus());
            verify(competitorRepository).save(competitor);
            verify(competitorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws 409 if the competitor has already retired")
        void alreadyRetired_throws409() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.RETIRED, CompetitorType.DWARF, 5);
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));

            assertThrows(ConflictException.class, () -> competitorService.retire(competitor.getId()));
            verify(competitorRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 404 if the competitor does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(competitorRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> competitorService.retire(id));
        }
    }

    // ============================== delete (physical delete) ===============================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("permanently removes a competitor with no official race results")
        void deletesWithNoHistory() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));

            competitorService.delete(competitor.getId());

            verify(competitorRepository).delete(competitor);
        }

        @Test
        @DisplayName("throws 409 if the competitor has official race results")
        void hasHistory_throws409() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 2);
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));

            assertThrows(ConflictException.class, () -> competitorService.delete(competitor.getId()));
            verify(competitorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws 404 if the competitor does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(competitorRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> competitorService.delete(id));
        }
    }

    // ============================== patch ===============================

    @Nested
    @DisplayName("patch")
    class Patch {

        @Test
        @DisplayName("changes only the provided fields")
        void changesProvidedFields() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.existsByNicknameIgnoreCaseAndIdNot("JD2", competitor.getId())).thenReturn(false);
            when(competitorRepository.save(any(Competitor.class))).thenAnswer(i -> i.getArgument(0));

            CompetitorPatchRequest request = new CompetitorPatchRequest(
                    null, "JD2", null, null, null, null, null, null);

            CompetitorResponse result = competitorService.patch(competitor.getId(), request);

            assertEquals("JD2", result.nickname());
            // Untouched fields keep their original values.
            assertEquals("John D", result.name());
        }

        @Test
        @DisplayName("throws 400 if the request has no fields to update")
        void emptyRequest_throws400() {
            CompetitorPatchRequest request = new CompetitorPatchRequest(
                    null, null, null, null, null, null, null, null);

            assertThrows(BadRequestException.class,
                    () -> competitorService.patch(UUID.randomUUID(), request));
        }

        @Test
        @DisplayName("throws 409 if the new nickname is already taken")
        void duplicateNickname_throws409() {
            Competitor competitor = buildCompetitor("JD", CompetitorStatus.ACTIVE, CompetitorType.DWARF, 0);
            when(competitorRepository.findWithTeamById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.existsByNicknameIgnoreCaseAndIdNot("Taken", competitor.getId())).thenReturn(true);

            CompetitorPatchRequest request = new CompetitorPatchRequest(
                    null, "Taken", null, null, null, null, null, null);

            assertThrows(ConflictException.class, () -> competitorService.patch(competitor.getId(), request));
        }

        @Test
        @DisplayName("throws 404 if the competitor does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(competitorRepository.findWithTeamById(id)).thenReturn(Optional.empty());

            CompetitorPatchRequest request = new CompetitorPatchRequest(
                    null, "JD2", null, null, null, null, null, null);

            assertThrows(ResourceNotFoundException.class, () -> competitorService.patch(id, request));
        }
    }

    // ============================== Helpers ===============================

    private Competitor buildCompetitor(String nickname, CompetitorStatus status, CompetitorType type, int completedRaces) {
        return Competitor.builder()
                .id(UUID.randomUUID()).name("John D").nickname(nickname)
                .competitorType(type).birthDate(LocalDate.of(1989, 6, 15))
                .height(120.0).weight(50.0).origin("USA")
                .status(status).completedRaces(completedRaces)
                .build();
    }
}