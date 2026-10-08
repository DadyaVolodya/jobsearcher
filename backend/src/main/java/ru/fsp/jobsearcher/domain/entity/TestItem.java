package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
import ru.fsp.jobsearcher.domain.enums.QuestionType;

@Getter
@Setter
@Entity
@Table(name = "test_item")
public class TestItem {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "industry_code", nullable = false)
    private String industryCode;

    @Column(name = "specialization_code")
    private String specializationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "prompt_template", nullable = false)
    private String promptTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variants_json", nullable = false)
    private List<Map<String, Object>> variants = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_pool_json", nullable = false)
    private List<Map<String, Object>> optionsPool = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer_json")
    private Map<String, Object> correctAnswer = new HashMap<>();

    private String rubric;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String section = "B";

    @Column(name = "anti_ai_prompt")
    private String antiAiPrompt;

    @Column(name = "captcha_style", nullable = false)
    private boolean captchaStyle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "param_schema_json", nullable = false)
    private Map<String, Object> paramSchema = new HashMap<>();
}
