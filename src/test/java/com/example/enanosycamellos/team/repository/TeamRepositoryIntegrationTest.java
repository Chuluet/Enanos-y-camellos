package com.example.enanosycamellos.team.repository;

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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class TeamRepositoryIntegrationTest {

    @Autowired
    private ITeamRepository teamRepository;

    @Autowired
    private TestEntityManager em;

    private Competitor competitor;
    private Competitor competitor2;
    private Team activeTeam;
    private Team inactiveTeam;

    @BeforeEach
    void setUp() {
        activeTeam = Team.builder()
                .name("The Five Exceptions").description("A great team").coach("Sebas")
                .status(TeamStatus.ACTIVE).maxMembers(2).build();
        em.persistAndFlush(activeTeam);

        inactiveTeam = Team.builder()
                .name("Inactive Team").description("An inactive team").coach("Sebas")
                .status(TeamStatus.INACTIVE).maxMembers(2).build();
        em.persistAndFlush(inactiveTeam);

        competitor = Competitor.builder()
                .name("John D").nickname("JD").competitorType(CompetitorType.DWARF)
                .birthDate(LocalDate.of(1989, 6, 15)).age(34).height(120.0).weight(50.0)
                .origin("USA").status(CompetitorStatus.ACTIVE).team(activeTeam).build();
        em.persistAndFlush(competitor);

        competitor2 = Competitor.builder()
                .name("Paco Esteban").nickname("PE").competitorType(CompetitorType.DWARF)
                .birthDate(LocalDate.of(1989, 6, 15)).age(34).height(120.0).weight(50.0)
                .origin("USA").status(CompetitorStatus.ACTIVE).team(activeTeam).build();
        em.persistAndFlush(competitor2);

        em.clear();
    }

    @Test
    @DisplayName("findAllActiveWithMembers returns only active teams with their members")
    void findAllActiveWithMembers_returnsOnlyActive() {
        List<Team> result = teamRepository.findAllWithMembers(TeamStatus.ACTIVE);
        assertEquals(1, result.size());
        assertEquals("The Five Exceptions", result.getFirst().getName());
        assertEquals(2, result.getFirst().getMembers().size());
        assertFalse(result.getFirst().getMembers().isEmpty());
    }

    @Test
    @DisplayName("findWithMembersById finds the team regardless of its status")
    void findWithMembersById_findsInactiveToo() {
        Optional<Team> result = teamRepository.findWithMembersById(inactiveTeam.getId());

        assertTrue(result.isPresent());
        assertEquals("Inactive Team", result.get().getName());
    }

    @Test
    @DisplayName("existsByName detects duplicate (case insensitive)")
    void existsByName_caseInsensitive() {
        boolean exists = teamRepository
                .existsByNameIgnoreCase("the five exceptions");
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByName with IdNot excludes the team being updated")
    void existsByName_excludesSelf() {
        boolean exists = teamRepository
                .existsByNameIgnoreCaseAndIdNot("The Five Exceptions", activeTeam.getId());

        assertFalse(exists);
    }
}
