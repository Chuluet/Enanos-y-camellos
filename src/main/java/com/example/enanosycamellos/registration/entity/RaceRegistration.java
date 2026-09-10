package com.example.enanosycamellos.registration.entity;

import com.example.enanosycamellos.competitor.entity.Competitor;
import com.example.enanosycamellos.race.entity.Race;
import com.example.enanosycamellos.team.entity.Team;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A registration for a race, made either by a single {@link Competitor}
 * (individual race) or by a {@link Team} (team race). Exactly one of
 * {@code competitor}/{@code team} is set, never both, never neither — that
 * invariant is enforced in {@code RegistrationService}, not here.
 */
@Entity
@Table(name = "race_registrations", indexes = {
        @Index(name = "idx_registrations_race", columnList = "race_id"),
        @Index(name = "idx_registrations_status", columnList = "status")
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RaceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_id")
    private Competitor competitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "starting_position")
    private Integer startingPosition;

    @Column(name = "validation_notes", length = 500)
    private String validationNotes;

    @Column(name = "registered_by", nullable = false, length = 100)
    private String registeredBy;

    @PrePersist
    void onCreate() {
        registrationDate = LocalDateTime.now();
    }

    /** True if this registration is for a competitor, not a team. */
    @Transient
    public boolean isIndividual() {
        return competitor != null;
    }
}