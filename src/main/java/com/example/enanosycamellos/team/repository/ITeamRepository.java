package com.example.enanosycamellos.team.repository;

import com.example.enanosycamellos.team.entity.Team;
import com.example.enanosycamellos.team.entity.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITeamRepository extends JpaRepository<Team, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** Teams whose status is not the given one (e.g. exclude INACTIVE), used by getTeams(). */
    List<Team> findAllByStatusNot(TeamStatus status);

    @Query("""
        SELECT t FROM Team t
        LEFT JOIN FETCH t.members
        WHERE t.status = :status
        ORDER BY t.name
        """)
    List<Team> findAllWithMembers(@Param("status") TeamStatus status);

    /** A team with its members already loaded, in a single query. */
    @Query("""
        SELECT t FROM Team t
        LEFT JOIN FETCH t.members
        WHERE t.id = :id
        """)
    Optional<Team> findWithMembersById(@Param("id") UUID id);
}