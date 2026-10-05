package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.EmployerNeed;

public interface EmployerNeedRepository extends JpaRepository<EmployerNeed, UUID> {
    List<EmployerNeed> findByEmployerIdAndActiveTrue(UUID employerId);
}
