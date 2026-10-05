package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.VacancyApplication;

public interface VacancyApplicationRepository extends JpaRepository<VacancyApplication, UUID> {
    List<VacancyApplication> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
    List<VacancyApplication> findByVacancyIdOrderByCreatedAtDesc(UUID vacancyId);
    Optional<VacancyApplication> findByVacancyIdAndCandidateId(UUID vacancyId, UUID candidateId);
}
