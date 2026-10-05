package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import ru.fsp.jobsearcher.domain.enums.InvitationStatus;

@Getter
@Setter
@Entity
@Table(name = "invitation")
public class Invitation {

    @Id
    private UUID id;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "need_id")
    private UUID needId;

    @Column(nullable = false)
    private String message;

    @Column(name = "salary_from", nullable = false)
    private int salaryFrom;

    @Column(name = "salary_to", nullable = false)
    private int salaryTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
