package com.example.enanosycamellos.race.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "races", indexes = {
        @Index(name = "idx_races_status", columnList = "status"),
        @Index(name = "idx_races_scheduled_date_time", columnList = "scheduled_date_time")
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "scheduled_date_time", nullable = false)
    private LocalDateTime scheduledDateTime;

    @Column(name = "start_location", nullable = false, length = 150)
    private String startLocation;

    @Column(name = "finish_location", nullable = false, length = 150)
    private String finishLocation;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(name = "race_type", nullable = false, length = 20)
    private RaceType raceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RaceStatus status = RaceStatus.DRAFT;

    @Column(nullable = false, length = 100)
    private String organizer;

    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}