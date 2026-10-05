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
@Table(name = "grade_change_log")
public class GradeChangeLog {

    @Id
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "from_grade")
    private String fromGrade;

    @Column(name = "to_grade", nullable = false)
    private String toGrade;

    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
