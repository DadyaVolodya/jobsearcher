package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fsp.jobsearcher.domain.enums.SessionStatus;

@Getter
@Setter
@Entity
@Table(name = "survey_session")
public class SurveySession {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "questionnaire_id", nullable = false)
    private UUID questionnaireId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers_json", nullable = false)
    private Map<String, Object> answers = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_vector_json", nullable = false)
    private Map<String, Double> resultVector = new HashMap<>();

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
