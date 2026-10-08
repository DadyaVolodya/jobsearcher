package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.SessionStatus;

@Getter
@Setter
@Entity
@Table(name = "test_session")
public class TestSession {

    @Id
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_grade", nullable = false)
    private Grade targetGrade;

    @Column(name = "industry_code", nullable = false)
    private String industryCode;

    @Column(name = "specialization_code", nullable = false)
    private String specializationCode;

    @Column(nullable = false)
    private long seed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_json", nullable = false)
    private List<Map<String, Object>> items = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers_json", nullable = false)
    private Map<String, Object> answers = new HashMap<>();

    private Double score;
    private Boolean passed;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(name = "grade_confirmed")
    private Boolean gradeConfirmed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_scores_json", nullable = false)
    private Map<String, Double> sectionScores = new HashMap<>();

    @Column(name = "junior_floor_score")
    private Double juniorFloorScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proctor_events_json", nullable = false)
    private List<Map<String, Object>> proctorEvents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_grade")
    private Grade assignedGrade;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
