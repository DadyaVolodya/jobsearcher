package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.Industry;

public interface IndustryRepository extends JpaRepository<Industry, String> {
    List<Industry> findAllByOrderBySortOrderAsc();
}
