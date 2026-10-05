package ru.fsp.jobsearcher.application.security;

import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.domain.entity.AppUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.AppUserRepository;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.EmployerProfileRepository;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final AppUserRepository appUserRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;

    @Transactional
    public CurrentUser ensureUser(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        String sub = jwt.getSubject();
        String email = firstNonBlank(jwt.getClaimAsString("email"), jwt.getClaimAsString("preferred_username"), sub);
        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));
        UserRole role = resolveRole(jwt, authorities);

        AppUser user = appUserRepository.findByKeycloakSub(sub).orElseGet(() -> {
            AppUser created = AppUser.create(sub, email, role, emailVerified);
            appUserRepository.save(created);
            if (role == UserRole.CANDIDATE) {
                candidateProfileRepository.save(CandidateProfile.create(created.getId()));
            } else if (role == UserRole.EMPLOYER) {
                employerProfileRepository.save(EmployerProfile.create(created.getId(), "Новая компания"));
            }
            return created;
        });

        boolean dirty = false;
        if (!email.equals(user.getEmail())) {
            user.setEmail(email);
            dirty = true;
        }
        if (user.isEmailVerified() != emailVerified) {
            user.setEmailVerified(emailVerified);
            dirty = true;
        }
        if (user.getRole() != role && role != UserRole.ADMIN) {
            // keep persisted role unless elevating via realm
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()))) {
                user.setRole(role);
                dirty = true;
            }
        }
        if (dirty) {
            user.setUpdatedAt(Instant.now());
            appUserRepository.save(user);
        }

        if (user.getRole() == UserRole.CANDIDATE) {
            candidateProfileRepository.findByUserId(user.getId())
                    .orElseGet(() -> candidateProfileRepository.save(CandidateProfile.create(user.getId())));
        } else if (user.getRole() == UserRole.EMPLOYER) {
            employerProfileRepository.findByUserId(user.getId())
                    .orElseGet(() -> employerProfileRepository.save(EmployerProfile.create(user.getId(), "Новая компания")));
        }

        return new CurrentUser(user.getId(), user.getKeycloakSub(), user.getEmail(), user.getRole(), user.isEmailVerified());
    }

    public CurrentUser loadByUserId(UUID userId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        return new CurrentUser(user.getId(), user.getKeycloakSub(), user.getEmail(), user.getRole(), user.isEmailVerified());
    }

    private static UserRole resolveRole(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority a : authorities) {
            String auth = a.getAuthority();
            if ("ROLE_ADMIN".equals(auth)) {
                return UserRole.ADMIN;
            }
            if ("ROLE_EMPLOYER".equals(auth)) {
                return UserRole.EMPLOYER;
            }
            if ("ROLE_CANDIDATE".equals(auth)) {
                return UserRole.CANDIDATE;
            }
        }
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> map) {
            Object roles = map.get("roles");
            if (roles instanceof Collection<?> col) {
                for (Object r : col) {
                    String role = String.valueOf(r).toUpperCase(Locale.ROOT);
                    if (role.contains("ADMIN")) {
                        return UserRole.ADMIN;
                    }
                    if (role.contains("EMPLOYER")) {
                        return UserRole.EMPLOYER;
                    }
                    if (role.contains("CANDIDATE")) {
                        return UserRole.CANDIDATE;
                    }
                }
            }
        }
        return UserRole.CANDIDATE;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "unknown";
    }
}
