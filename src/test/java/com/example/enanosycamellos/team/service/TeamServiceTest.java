package com.example.enanosycamellos.team.service;

import com.example.enanosycamellos.common.exceptions.BadRequestException;
import com.example.enanosycamellos.common.exceptions.ConflictException;
import com.example.enanosycamellos.common.exceptions.ResourceNotFoundException;
import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.competitor.repository.ICompetitorRepository;
import com.example.enanosycamellos.team.dto.TeamPatchRequest;
import com.example.enanosycamellos.team.dto.TeamRequest;
import com.example.enanosycamellos.team.dto.TeamResponse;
import com.example.enanosycamellos.team.dto.TeamUpdateRequest;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import com.example.enanosycamellos.team.repository.ITeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Team service unit tests. Both repositories are mocked to isolate the
 * business logic, same approach as CowServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private ITeamRepository teamRepository;

    @Mock
    private ICompetitorRepository competitorRepository;

    @InjectMocks
    private TeamService teamService;

    // ============================== Read ===============================

    @Nested
    @DisplayName("getTeams")
    class GetTeams {

        @Test
        @DisplayName("returns all non-inactive teams")
        void returnsAllNonInactive() {
            Team team = buildTeam("The Five Exceptions");
            when(teamRepository.findAllByStatusNot(TeamStatus.INACTIVE))
                    .thenReturn(List.of(team));

            List<TeamResponse> result = teamService.getTeams();

            assertEquals(1, result.size());
            assertEquals("The Five Exceptions", result.getFirst().name());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns the team if it exists")
        void returnsTeam() {
            Team team = buildTeam("The Five Exceptions");
            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));

            TeamResponse result = teamService.getById(team.getId());

            assertEquals("The Five Exceptions", result.name());
        }

        @Test
        @DisplayName("throws 404 if it does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(teamRepository.findWithMembersById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> teamService.getById(id));
        }
    }

    // ============================== Creation ==============================

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates the team with no members")
        void savesWithNoMembers() {
            TeamRequest request = new TeamRequest(
                    "The Five Exceptions", "desc", "Mr. Abandonado", 5, TeamStatus.ACTIVE, null);

            when(teamRepository.existsByNameIgnoreCase("The Five Exceptions")).thenReturn(false);
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team t = invocation.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });

            TeamResponse result = teamService.create(request);

            assertNotNull(result);
            assertEquals("The Five Exceptions", result.name());
            verify(teamRepository).save(any(Team.class));
            verifyNoInteractions(competitorRepository);
        }

        @Test
        @DisplayName("creates the team with initial members")
        void savesWithMembers() {
            UUID competitorId = UUID.randomUUID();
            TeamRequest request = new TeamRequest(
                    "The Five Exceptions", "desc", "Mr. Abandonado", 5, TeamStatus.ACTIVE,
                    List.of(competitorId));

            Competitor competitor = buildCompetitor(competitorId, CompetitorStatus.ACTIVE, null);

            when(teamRepository.existsByNameIgnoreCase("The Five Exceptions")).thenReturn(false);
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team t = invocation.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });
            when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(competitor));

            TeamResponse result = teamService.create(request);

            assertEquals(1, result.members().size());
            verify(competitorRepository).save(competitor);
        }

        @Test
        @DisplayName("throws 409 if the name already exists")
        void duplicateName_throws409() {
            TeamRequest request = new TeamRequest(
                    "The Five Exceptions", null, "Mr. Abandonado", 5, null, null);
            when(teamRepository.existsByNameIgnoreCase("The Five Exceptions")).thenReturn(true);

            assertThrows(ConflictException.class, () -> teamService.create(request));
            verify(teamRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 400 if there are more member ids than maxMembers")
        void tooManyMembers_throws400() {
            TeamRequest request = new TeamRequest(
                    "The Five Exceptions", null, "Mr. Abandonado", 1, TeamStatus.ACTIVE,
                    List.of(UUID.randomUUID(), UUID.randomUUID()));

            when(teamRepository.existsByNameIgnoreCase("The Five Exceptions")).thenReturn(false);
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team t = invocation.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });

            assertThrows(BadRequestException.class, () -> teamService.create(request));
        }

        @Test
        @DisplayName("throws 404 if a member id does not exist")
        void memberNotFound_throws404() {
            UUID competitorId = UUID.randomUUID();
            TeamRequest request = new TeamRequest(
                    "The Five Exceptions", null, "Mr. Abandonado", 5, TeamStatus.ACTIVE,
                    List.of(competitorId));

            when(teamRepository.existsByNameIgnoreCase("The Five Exceptions")).thenReturn(false);
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team t = invocation.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });
            when(competitorRepository.findById(competitorId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> teamService.create(request));
        }
    }

    // ============================== Update (PUT, full replace) =========================

    @Nested
    @DisplayName("update (PUT)")
    class Update {

        @Test
        @DisplayName("replaces every field")
        void replacesAllFields() {
            Team team = buildTeam("The Five Exceptions");
            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.existsByNameIgnoreCaseAndIdNot("The Five Exceptions II", team.getId()))
                    .thenReturn(false);
            when(teamRepository.save(any(Team.class))).thenAnswer(i -> i.getArgument(0));

            TeamUpdateRequest request = new TeamUpdateRequest(
                    "The Five Exceptions II", "new desc", "New Coach", 8, TeamStatus.SUSPENDED);
            TeamResponse result = teamService.update(team.getId(), request);

            assertEquals("The Five Exceptions II", result.name());
            assertEquals("new desc", result.description());
            assertEquals("New Coach", result.coach());
            assertEquals(8, result.maxMembers());
            assertEquals(TeamStatus.SUSPENDED, result.status());
        }

        @Test
        @DisplayName("throws 404 if the team does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(teamRepository.findWithMembersById(id)).thenReturn(Optional.empty());

            TeamUpdateRequest request = new TeamUpdateRequest(
                    "Name", "desc", "Coach", 5, TeamStatus.ACTIVE);

            assertThrows(ResourceNotFoundException.class, () -> teamService.update(id, request));
        }

        @Test
        @DisplayName("throws 409 if the new name already exists")
        void duplicateName_throws409() {
            Team team = buildTeam("The Five Exceptions");
            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.existsByNameIgnoreCaseAndIdNot("Taken", team.getId())).thenReturn(true);

            TeamUpdateRequest request = new TeamUpdateRequest("Taken", "desc", "Coach", 5, TeamStatus.ACTIVE);

            assertThrows(ConflictException.class, () -> teamService.update(team.getId(), request));
        }

        @Test
        @DisplayName("throws 409 if maxMembers goes below the current member count")
        void maxMembersBelowCurrent_throws409() {
            Team team = buildTeam("The Five Exceptions");
            team.addMember(buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, null));
            team.addMember(buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, null));

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.existsByNameIgnoreCaseAndIdNot("The Five Exceptions", team.getId()))
                    .thenReturn(false);

            TeamUpdateRequest request = new TeamUpdateRequest(
                    "The Five Exceptions", "desc", "Coach", 1, TeamStatus.ACTIVE);

            assertThrows(ConflictException.class, () -> teamService.update(team.getId(), request));
        }
    }

    // ============================== Patch (partial) =========================

    @Nested
    @DisplayName("patch")
    class Patch {

        @Test
        @DisplayName("changes only the provided fields")
        void changesProvidedFields() {
            Team team = buildTeam("The Five Exceptions");
            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.existsByNameIgnoreCaseAndIdNot("The Five Exceptions II", team.getId()))
                    .thenReturn(false);
            when(teamRepository.save(any(Team.class))).thenAnswer(i -> i.getArgument(0));

            TeamPatchRequest request = new TeamPatchRequest(
                    "The Five Exceptions II", null, null, null, null);
            TeamResponse result = teamService.patch(team.getId(), request);

            assertEquals("The Five Exceptions II", result.name());
            // Untouched fields keep their original values.
            assertEquals("desc", result.description());
            assertEquals("Mr. Abandonado", result.coach());
        }

        @Test
        @DisplayName("throws 400 if the request is empty")
        void emptyRequest_throws400() {
            TeamPatchRequest request = new TeamPatchRequest(null, null, null, null, null);

            assertThrows(BadRequestException.class,
                    () -> teamService.patch(UUID.randomUUID(), request));
        }

        @Test
        @DisplayName("throws 409 if the new name already exists")
        void duplicateName_throws409() {
            Team team = buildTeam("The Five Exceptions");
            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.existsByNameIgnoreCaseAndIdNot("Taken", team.getId())).thenReturn(true);

            assertThrows(ConflictException.class,
                    () -> teamService.patch(team.getId(),
                            new TeamPatchRequest("Taken", null, null, null, null)));
        }

        @Test
        @DisplayName("throws 409 if maxMembers goes below the current member count")
        void maxMembersBelowCurrent_throws409() {
            Team team = buildTeam("The Five Exceptions");
            team.addMember(buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, null));

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));

            assertThrows(ConflictException.class,
                    () -> teamService.patch(team.getId(),
                            new TeamPatchRequest(null, null, null, 0, null)));
        }
    }

    // ============================== addMember =======================

    @Nested
    @DisplayName("addMember")
    class AddMember {

        @Test
        @DisplayName("adds an eligible competitor to the team")
        void addsEligibleCompetitor() {
            Team team = buildTeam("The Five Exceptions");
            Competitor competitor = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, null);

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.save(competitor)).thenReturn(competitor);

            TeamResponse result = teamService.addMember(team.getId(), competitor.getId());

            assertEquals(1, result.members().size());
            assertSame(team, competitor.getTeam());
        }

        @Test
        @DisplayName("throws 409 if the team is inactive")
        void inactiveTeam_throws409() {
            Team team = buildTeam("The Five Exceptions");
            team.setStatus(TeamStatus.INACTIVE);
            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));

            assertThrows(ConflictException.class,
                    () -> teamService.addMember(team.getId(), UUID.randomUUID()));
        }

        @Test
        @DisplayName("throws 409 if the team is already full")
        void fullTeam_throws409() {
            Team team = Team.builder()
                    .id(UUID.randomUUID()).name("Tiny Team").status(TeamStatus.ACTIVE)
                    .maxMembers(1).build();
            team.addMember(buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, null));

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));

            assertThrows(ConflictException.class,
                    () -> teamService.addMember(team.getId(), UUID.randomUUID()));
        }

        @Test
        @DisplayName("throws 400 if the competitor is not active")
        void inactiveCompetitor_throws400() {
            Team team = buildTeam("The Five Exceptions");
            Competitor competitor = buildCompetitor(UUID.randomUUID(), CompetitorStatus.INJURED, null);

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));

            assertThrows(BadRequestException.class,
                    () -> teamService.addMember(team.getId(), competitor.getId()));
        }

        @Test
        @DisplayName("throws 409 if the competitor already belongs to a team")
        void alreadyInTeam_throws409() {
            Team otherTeam = buildTeam("Other Team");
            Team team = buildTeam("The Five Exceptions");
            Competitor competitor = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, otherTeam);

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));

            assertThrows(ConflictException.class,
                    () -> teamService.addMember(team.getId(), competitor.getId()));
        }

        @Test
        @DisplayName("throws 404 if the team does not exist")
        void teamNotFound_throws404() {
            UUID teamId = UUID.randomUUID();
            when(teamRepository.findWithMembersById(teamId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> teamService.addMember(teamId, UUID.randomUUID()));
        }
    }

    // ============================== removeMember =======================

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("removes the competitor from the team")
        void removesCompetitor() {
            Team team = buildTeam("The Five Exceptions");
            Competitor competitor = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, null);
            team.addMember(competitor);

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
            when(competitorRepository.save(competitor)).thenReturn(competitor);

            TeamResponse result = teamService.removeMember(team.getId(), competitor.getId());

            assertTrue(result.members().isEmpty());
            assertNull(competitor.getTeam());
        }

        @Test
        @DisplayName("throws 404 if the competitor does not belong to that team")
        void notAMember_throws404() {
            Team team = buildTeam("The Five Exceptions");
            Competitor competitor = buildCompetitor(UUID.randomUUID(), CompetitorStatus.ACTIVE, null);

            when(teamRepository.findWithMembersById(team.getId())).thenReturn(Optional.of(team));
            when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));

            assertThrows(ResourceNotFoundException.class,
                    () -> teamService.removeMember(team.getId(), competitor.getId()));
        }
    }

    // ============================== deactivate ===========================

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("sets the team's status to INACTIVE")
        void setsInactive() {
            Team team = buildTeam("The Five Exceptions");
            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));

            teamService.deactivate(team.getId());

            assertEquals(TeamStatus.INACTIVE, team.getStatus());
            verify(teamRepository).save(team);
        }

        @Test
        @DisplayName("throws 404 if the team does not exist")
        void notFound_throws404() {
            UUID id = UUID.randomUUID();
            when(teamRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> teamService.deactivate(id));
        }
    }

    // ============================== Helpers ===============================

    private Team buildTeam(String name) {
        return Team.builder()
                .id(UUID.randomUUID()).name(name).description("desc").coach("Mr. Abandonado")
                .status(TeamStatus.ACTIVE).victories(0).defeats(0).maxMembers(5)
                .build();
    }

    private Competitor buildCompetitor(UUID id, CompetitorStatus status, Team team) {
        return Competitor.builder()
                .id(id).name("Null Pointer").nickname("NP-" + id)
                .competitorType(CompetitorType.DWARF).status(status).team(team)
                .build();
    }
}