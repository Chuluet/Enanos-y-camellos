package com.example.enanosycamellos.competitor.repository;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.competitor.entity.CompetitorStatus;
import com.example.enanosycamellos.competitor.entity.CompetitorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link Competitor}.
 *
 * <p>Unlike {@code ICowRepository} / {@code ITeamRepository}, the Module 2
 * requirements explicitly demand pagination, filtering and sorting
 * ("Filtering, pagination and sorting are mandatory"). That's why most
 * queries here return {@code Page<Competitor>} instead of {@code List},
 * so the controller can pass a {@code Pageable} straight through.</p>
 *
 * <p>{@code @EntityGraph} is used instead of {@code JOIN FETCH} (as in
 * {@code ICowRepository}) because {@code JOIN FETCH} combined with
 * {@code Pageable} makes Hibernate paginate in memory instead of in the
 * database — it fetches everything first and slices the list in Java,
 * which defeats the purpose of pagination. {@code @EntityGraph} on a
 * {@code @ManyToOne} (team is single-valued, not a collection) avoids
 * that problem entirely while still resolving the N+1 query.</p>
 */
public interface ICompetitorRepository extends JpaRepository<Competitor, UUID> {

    /** A competitor with its team already loaded, in a single query. */
    @EntityGraph(attributePaths = "team")
    Optional<Competitor> findWithTeamById(UUID id);

    /** Exact search by nickname, ignoring case. */
    Optional<Competitor> findByNicknameIgnoreCase(String nickname);

    /**
     * Does a competitor with that nickname already exist?
     *
     * <p>Same reasoning as {@code existsByNameIgnoreCase} in
     * {@code ICowRepository}: the nickname column has a UNIQUE constraint at
     * the database level, so this check has to consider every row, not just
     * "active" ones (Competitor doesn't even have a logical-delete flag —
     * see {@code CompetitorStatus} instead).</p>
     */
    boolean existsByNicknameIgnoreCase(String nickname);

    /** Same as above, excluding an id: used when updating. */
    boolean existsByNicknameIgnoreCaseAndIdNot(String nickname, UUID id);

    /** Paginated + filtered listing by status (e.g. only ACTIVE for race eligibility checks). */
    @EntityGraph(attributePaths = "team")
    Page<Competitor> findAllByStatus(CompetitorStatus status, Pageable pageable);

    /** Paginated + filtered listing by type (DWARF, CAMEL, MEDIUM, OTHER). */
    @EntityGraph(attributePaths = "team")
    Page<Competitor> findAllByCompetitorType(CompetitorType competitorType, Pageable pageable);

    /** Combined filter: both status and type at once. */
    @EntityGraph(attributePaths = "team")
    Page<Competitor> findAllByStatusAndCompetitorType(
            CompetitorStatus status, CompetitorType competitorType, Pageable pageable);

    /**
     * Overrides the inherited {@code findAll(Pageable)} to also resolve
     * {@code team} eagerly, so the unfiltered "list all" endpoint doesn't
     * trigger one extra query per row when the mapper calls
     * {@code competitor.getTeam()}.
     */
    @Override
    @EntityGraph(attributePaths = "team")
    Page<Competitor> findAll(Pageable pageable);

    /** All competitors currently in a given team — used by TeamService/TeamMapper if needed. */
    List<Competitor> findAllByTeamId(UUID teamId);
}