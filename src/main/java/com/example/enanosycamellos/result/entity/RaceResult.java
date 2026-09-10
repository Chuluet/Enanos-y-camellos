package com.example.enanosycamellos.result.entity;

import com.example.enanosycamellos.registration.entity.RaceRegistration;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Official result of a participant (competitor or team) in a race. One
 * result per registration — the registration already identifies "which
 * race, which competitor-or-team", so Result doesn't repeat that, it just
 * points to the registration via a unique FK.
 */
@Entity
@Table(name = "race_results", indexes = {
        @Index(name = "idx_results_registration", columnList = "registration_id")
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private RaceRegistration registration;

    @Column(name = "start_position")
    private Integer startPosition;

    @Column(name = "final_position")
    private Integer finalPosition;

    @Column(name = "completion_time")
    private Double completionTime;

    @Column(name = "penalty_time")
    @Builder.Default
    private Double penaltyTime = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status;

    @Column(length = 500)
    private String notes;

    @Column(name = "recorded_by", nullable = false, length = 100)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}