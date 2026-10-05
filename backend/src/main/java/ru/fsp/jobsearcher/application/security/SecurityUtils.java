package ru.fsp.jobsearcher.application.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.domain.enums.UserRole;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserProvisioningService userProvisioningService;

    public CurrentUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw ApiException.forbidden("Unauthorized");
        }
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return userProvisioningService.ensureUser(jwtAuth.getToken(), jwtAuth.getAuthorities());
        }
        if (auth.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser;
        }
        if (auth.getDetails() instanceof CurrentUser currentUser) {
            return currentUser;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return userProvisioningService.ensureUser(jwt, auth.getAuthorities());
        }
        // test / permit-all stub via headers is handled in DevAuthFilter
        throw ApiException.forbidden("Unauthorized");
    }

    public CurrentUser requireRole(UserRole role) {
        CurrentUser user = requireCurrentUser();
        if (user.role() != role && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Требуется роль " + role);
        }
        return user;
    }

    public UUID requireUserId() {
        return requireCurrentUser().userId();
    }
}
