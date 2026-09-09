package com.example.enanosycamellos.competitor.entity;

import com.example.enanosycamellos.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Competitor. Owning side of the N to 1 with {@link Team} (holds team_id),
 * same role Cow plays with Owner.
 *
 * <p>No bean-validation annotations here on purpose — validation belongs on
 * the DTOs that cross the API boundary, same as Cow/Owner/Team. Otherwise a
 * service-internal save before all fields are set can throw a raw
 * ConstraintViolationException instead of the controlled 400 response.</p>
 */
@Entity
@Table(name = "competitors")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Only this one is unique — Module 2: "name and unique nickname". */
    @Column(nullable = false, unique = true, length = 100)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "competitor_type", nullable = false, length = 20)
    private CompetitorType competitorType;

    /** Optional: Module 2 asks for birth date OR approximate age, not both. */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Approximate age, used when the exact birth date isn't known. */
    private Integer age;

    @Column(nullable = false)
    private Double height;

    @Column(nullable = false)
    private Double weight;

    /** "Country or place of origin" — matches the SQL column "origin". */
    @Column(nullable = false, length = 100)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CompetitorStatus status = CompetitorStatus.ACTIVE;

    @Column(name = "registration_date", nullable = false)
    @Builder.Default
    private LocalDate registrationDate = LocalDate.now();

    @Column(nullable = false)
    @Builder.Default
    private Integer victories = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer defeats = 0;

    @Column(name = "completed_races", nullable = false)
    @Builder.Default
    private Integer completedRaces = 0;

    /** Optional — Module 2: "Optional team." */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "team_id")
    private Team team;
}