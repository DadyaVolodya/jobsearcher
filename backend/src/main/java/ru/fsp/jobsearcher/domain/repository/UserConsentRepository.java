package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.UserConsent;
import ru.fsp.jobsearcher.domain.enums.ConsentType;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    Optional<UserConsent> findByUserIdAndConsentType(UUID userId, ConsentType type);
    List<UserConsent> findByUserId(UUID userId);
}
