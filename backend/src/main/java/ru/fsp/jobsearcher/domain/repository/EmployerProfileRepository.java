package ru.fsp.jobsearcher.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;

public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, UUID> {
    Optional<EmployerProfile> findByUserId(UUID userId);
}
