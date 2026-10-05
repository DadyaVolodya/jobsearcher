package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fsp_achievement")
public class FspAchievement {

    @Id
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "fsp_participant_id")
    private String fspParticipantId;

    @Column(nullable = false)
    private String title;

    @Column(name = "event_name")
    private String eventName;

    private Integer place;

    @Column(nullable = false)
    private int points;

    @Column(name = "achieved_at")
    private LocalDate achievedAt;

    @Column(nullable = false)
    private boolean verified = true;
}
