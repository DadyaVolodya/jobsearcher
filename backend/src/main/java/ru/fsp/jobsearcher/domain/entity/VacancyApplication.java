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
import ru.fsp.jobsearcher.domain.enums.ApplicationStatus;

@Getter
@Setter
@Entity
@Table(name = "vacancy_application")
public class VacancyApplication {

    @Id
    private UUID id;

    @Column(name = "vacancy_id", nullable = false)
    private UUID vacancyId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "cover_letter")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
