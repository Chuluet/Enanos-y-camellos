package com.example.enanosycamellos.team.entity;

import com.example.enanosycamellos.competitor.entity.Competitor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "creation_date", nullable = false)
    @Builder.Default
    private LocalDate creationDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TeamStatus status = TeamStatus.ACTIVE;

    @Column(nullable = false, length = 100)
    private String coach;

    @Column(nullable = false)
    @Builder.Default
    private Integer victories = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer defeats = 0;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    /**
     * Lado inverso de la relación N a 1 (Competitor es el dueño, tiene la FK team_id).
     * mappedBy = "team": cambios aquí no se persisten solos, hay que usar
     * addMember/removeMember para mantener ambos lados sincronizados.
     */
    @OneToMany(
            mappedBy = "team",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Competitor> members = new ArrayList<>();

    public void addMember(Competitor competitor) {
        members.add(competitor);
        competitor.setTeam(this);
    }

    public void removeMember(Competitor competitor) {
        members.remove(competitor);
        competitor.setTeam(null);
    }

    public boolean hasMember(Competitor competitor) {
        return members.contains(competitor);
    }
}