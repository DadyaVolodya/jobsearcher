package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fsp.jobsearcher.domain.enums.QuestionType;

@Getter
@Setter
@Entity
@Table(name = "survey_question")
public class SurveyQuestion {

    @Id
    private UUID id;

    @Column(name = "questionnaire_id", nullable = false)
    private UUID questionnaireId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(nullable = false)
    private double weight;

    @Column(name = "scale_key", nullable = false)
    private String scaleKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", nullable = false)
    private List<Map<String, Object>> options = new ArrayList<>();

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
