package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.Specialization;

public interface SpecializationRepository extends JpaRepository<Specialization, String> {
    List<Specialization> findByIndustryCodeOrderBySortOrderAsc(String industryCode);
    List<Specialization> findAllByOrderBySortOrderAsc();
}
