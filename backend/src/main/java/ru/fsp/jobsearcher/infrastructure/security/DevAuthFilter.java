package ru.fsp.jobsearcher.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.AppUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.AppUserRepository;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.EmployerProfileRepository;

/**
 * Dev/test auth via headers when app.security.permit-all=true.
 * Must be registered inside SecurityFilterChain (see SecurityConfig / TestSecurityConfig).
 */
@Component
@ConditionalOnProperty(prefix = "app.security", name = "permit-all", havingValue = "true")
@RequiredArgsConstructor
public class DevAuthFilter extends OncePerRequestFilter {

    private final AppUserRepository appUserRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String email = request.getHeader("X-User-Email");
        if (email != null && !email.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserRole role = UserRole.valueOf(
                    (request.getHeader("X-User-Role") == null ? "CANDIDATE" : request.getHeader("X-User-Role"))
                            .toUpperCase());
            String sub = request.getHeader("X-User-Sub");
            if (sub == null || sub.isBlank()) {
                sub = "dev-" + email;
            }
            final String keycloakSub = sub;
            AppUser user = appUserRepository.findByKeycloakSub(keycloakSub).orElseGet(() ->
                    appUserRepository.findByEmail(email).orElseGet(() -> {
                        AppUser created = AppUser.create(keycloakSub, email, role, true);
                        appUserRepository.save(created);
                        if (role == UserRole.CANDIDATE) {
                            candidateProfileRepository.save(CandidateProfile.create(created.getId()));
                        } else if (role == UserRole.EMPLOYER) {
                            employerProfileRepository.save(EmployerProfile.create(created.getId(), "Dev Company"));
                        }
                        return created;
                    }));
            CurrentUser current = new CurrentUser(user.getId(), user.getKeycloakSub(), user.getEmail(), user.getRole(), true);
            var auth = new UsernamePasswordAuthenticationToken(
                    current,
                    "N/A",
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            auth.setDetails(current);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
