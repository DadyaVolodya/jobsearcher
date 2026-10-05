package ru.fsp.jobsearcher.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.VacancyStatus;

@Getter
@Setter
@Entity
@Table(name = "vacancy")
public class Vacancy {

    @Id
    private UUID id;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "industry_code", nullable = false)
    private String industryCode;

    @Column(name = "specialization_code", nullable = false)
    private String specializationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stack_json", nullable = false)
    private List<String> stack = new ArrayList<>();

    @Column(name = "salary_from", nullable = false)
    private int salaryFrom;

    @Column(name = "salary_to", nullable = false)
    private int salaryTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VacancyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
