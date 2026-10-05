package ru.fsp.jobsearcher.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByKeycloakSub(String keycloakSub);
    Optional<AppUser> findByEmail(String email);
}
