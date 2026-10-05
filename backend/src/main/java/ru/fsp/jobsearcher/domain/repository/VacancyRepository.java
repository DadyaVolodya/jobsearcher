package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.Vacancy;
import ru.fsp.jobsearcher.domain.enums.VacancyStatus;

public interface VacancyRepository extends JpaRepository<Vacancy, UUID> {
    List<Vacancy> findByStatus(VacancyStatus status);
    List<Vacancy> findByEmployerIdOrderByCreatedAtDesc(UUID employerId);
}
