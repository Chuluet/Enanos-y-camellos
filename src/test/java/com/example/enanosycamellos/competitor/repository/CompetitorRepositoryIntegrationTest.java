package com.example.enanosycamellos.competitor.repository;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository integration tests for {@link ICompetitorRepository}, same
 * {@code @DataJpaTest} approach as {@code TeamRepositoryIntegrationTest}.
 * Focused on the derived-query methods, especially the RETIRED-exclusion
 * behavior added to back the default (no-filter) listing.
 */
@DataJpaTest
@ActiveProfiles("test")
class CompetitorRepositoryIntegrationTest {

    @Autowired
    private ICompetitorRepository competitorRepository;

    @Autowired
    private TestEntityManager em;

    private Team team;
    private Competitor active;
    private Competitor injured;
    private Competitor retired;

    @BeforeEach
    void setUp() {
        team = Team.builder()
                .name("The Five Exceptions").description("desc").coach("Sebas")
                .status(TeamStatus.ACTIVE).maxMembers(5).build();
        em.persistAndFlush(team);

        active = Competitor.builder()
                .name("John D").nickname("JD").competitorType(CompetitorType.DWARF)
                .birthDate(LocalDate.of(1989, 6, 15)).height(120.0).weight(50.0)
                .origin("USA").status(CompetitorStatus.ACTIVE).team(team).build();
        em.persistAndFlush(active);

        injured = Competitor.builder()
                .name("Paco Esteban").nickname("PE").competitorType(CompetitorType.CAMEL)
                .birthDate(LocalDate.of(1989, 6, 15)).height(180.0).weight(300.0)
                .origin("Egypt").status(CompetitorStatus.INJURED).build();
        em.persistAndFlush(injured);

        retired = Competitor.builder()
                .name("Old Timer").nickname("OT").competitorType(CompetitorType.DWARF)
                .birthDate(LocalDate.of(1970, 1, 1)).height(110.0).weight(45.0)
                .origin("USA").status(CompetitorStatus.RETIRED).build();
        em.persistAndFlush(retired);

        em.clear();
    }

    @Test
    @DisplayName("findAllByStatusNot excludes the given status")
    void findAllByStatusNot_excludesGivenStatus() {
        Page<Competitor> result = competitorRepository.findAllByStatusNot(CompetitorStatus.RETIRED, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().noneMatch(c -> c.getStatus() == CompetitorStatus.RETIRED));
    }

    @Test
    @DisplayName("findAllByCompetitorTypeAndStatusNot combines both filters")
    void findAllByCompetitorTypeAndStatusNot_combinesFilters() {
        Page<Competitor> result = competitorRepository
                .findAllByCompetitorTypeAndStatusNot(CompetitorType.DWARF, CompetitorStatus.RETIRED, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("JD", result.getContent().getFirst().getNickname());
    }

    @Test
    @DisplayName("findAllByStatusAndCompetitorType matches both filters exactly, including RETIRED when explicitly asked")
    void findAllByStatusAndCompetitorType_matchesBoth() {
        Page<Competitor> result = competitorRepository
                .findAllByStatusAndCompetitorType(CompetitorStatus.RETIRED, CompetitorType.DWARF, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("OT", result.getContent().getFirst().getNickname());
    }

    @Test
    @DisplayName("findAllByStatus alone still returns RETIRED competitors when asked explicitly")
    void findAllByStatus_includesRetiredWhenExplicit() {
        Page<Competitor> result = competitorRepository.findAllByStatus(CompetitorStatus.RETIRED, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("OT", result.getContent().getFirst().getNickname());
    }

    @Test
    @DisplayName("existsByNicknameIgnoreCase detects a duplicate regardless of case")
    void existsByNicknameIgnoreCase_caseInsensitive() {
        assertTrue(competitorRepository.existsByNicknameIgnoreCase("jd"));
    }

    @Test
    @DisplayName("existsByNicknameIgnoreCase returns false for a nickname that isn't taken")
    void existsByNicknameIgnoreCase_notTaken() {
        assertFalse(competitorRepository.existsByNicknameIgnoreCase("nobody"));
    }

    @Test
    @DisplayName("existsByNicknameIgnoreCaseAndIdNot excludes the competitor being updated")
    void existsByNicknameIgnoreCaseAndIdNot_excludesSelf() {
        boolean exists = competitorRepository.existsByNicknameIgnoreCaseAndIdNot("JD", active.getId());
        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByNicknameIgnoreCaseAndIdNot still detects a duplicate from another competitor")
    void existsByNicknameIgnoreCaseAndIdNot_detectsOthers() {
        boolean exists = competitorRepository.existsByNicknameIgnoreCaseAndIdNot("JD", injured.getId());
        assertTrue(exists);
    }

    @Test
    @DisplayName("findWithTeamById loads the team in the same query")
    void findWithTeamById_loadsTeam() {
        Optional<Competitor> result = competitorRepository.findWithTeamById(active.getId());

        assertTrue(result.isPresent());
        assertNotNull(result.get().getTeam());
        assertEquals("The Five Exceptions", result.get().getTeam().getName());
    }

    @Test
    @DisplayName("findWithTeamById returns empty for a teamless competitor's own lookup miss")
    void findWithTeamById_notFound() {
        Optional<Competitor> result = competitorRepository.findWithTeamById(java.util.UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findAllByTeamId returns only that team's members")
    void findAllByTeamId_returnsMembers() {
        List<Competitor> result = competitorRepository.findAllByTeamId(team.getId());

        assertEquals(1, result.size());
        assertEquals("JD", result.getFirst().getNickname());
    }
}