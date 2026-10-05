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
import ru.fsp.jobsearcher.domain.enums.UserRole;

@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, unique = true)
    private String keycloakSub;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AppUser create(String sub, String email, UserRole role, boolean emailVerified) {
        AppUser user = new AppUser();
        user.id = UUID.randomUUID();
        user.keycloakSub = sub;
        user.email = email;
        user.role = role;
        user.emailVerified = emailVerified;
        Instant now = Instant.now();
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }
}
