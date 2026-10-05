package ru.fsp.jobsearcher.application.security;

import java.util.UUID;
import ru.fsp.jobsearcher.domain.enums.UserRole;

public record CurrentUser(
        UUID userId,
        String keycloakSub,
        String email,
        UserRole role,
        boolean emailVerified
) {
}
