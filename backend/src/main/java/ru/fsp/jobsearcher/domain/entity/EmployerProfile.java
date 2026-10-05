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
@Table(name = "employer_profile")
public class EmployerProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    private String description;

    @Column(name = "industry_focus")
    private String industryFocus;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String website;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static EmployerProfile create(UUID userId, String companyName) {
        EmployerProfile p = new EmployerProfile();
        p.id = UUID.randomUUID();
        p.userId = userId;
        p.companyName = companyName;
        Instant now = Instant.now();
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
