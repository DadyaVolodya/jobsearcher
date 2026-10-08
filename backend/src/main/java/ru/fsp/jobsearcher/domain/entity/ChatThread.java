package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_thread")
public class ChatThread {

    @Id
    private UUID id;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "invitation_id", nullable = false, unique = true)
    private UUID invitationId;

    @Column(name = "vacancy_id")
    private UUID vacancyId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
