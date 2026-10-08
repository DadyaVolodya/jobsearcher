package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

@Getter
@Setter
@Entity
@Table(name = "candidate_profile")
public class CandidateProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "full_name")
    private String fullName;

    private String phone;
    private String city;
    private String about;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stack_json", nullable = false)
    private List<String> stack = new ArrayList<>();

    @Column(name = "experience_years")
    private BigDecimal experienceYears;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "soft_skills_json", nullable = false)
    private List<String> softSkills = new ArrayList<>();

    @Column(name = "resume_text")
    private String resumeText;

    @Column(name = "industry_code")
    private String industryCode;

    @Column(name = "specialization_code")
    private String specializationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "claimed_grade")
    private Grade claimedGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_grade")
    private Grade assignedGrade;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "profile_score", nullable = false)
    private double profileScore;

    @Column(name = "test_score", nullable = false)
    private double testScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "survey_vector_json", nullable = false)
    private Map<String, Double> surveyVector = new HashMap<>();

    @Column(name = "fsp_participant_id")
    private String fspParticipantId;

    /** Грейд из данных ФСП (олимпиады/достижения), влияет на приоритет в поиске. */
    @Enumerated(EnumType.STRING)
    @Column(name = "fsp_grade")
    private Grade fspGrade;

    @Column(name = "privacy_hide_contacts", nullable = false)
    private boolean privacyHideContacts = true;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "grade_confirmed", nullable = false)
    private boolean gradeConfirmed;

    @Column(name = "last_grade_change_at")
    private Instant lastGradeChangeAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static CandidateProfile create(UUID userId) {
        CandidateProfile p = new CandidateProfile();
        p.id = UUID.randomUUID();
        p.userId = userId;
        Instant now = Instant.now();
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
