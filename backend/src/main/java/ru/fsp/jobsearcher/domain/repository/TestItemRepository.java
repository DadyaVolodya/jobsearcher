package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.fsp.jobsearcher.domain.entity.TestItem;
import ru.fsp.jobsearcher.domain.enums.Grade;

public interface TestItemRepository extends JpaRepository<TestItem, UUID> {
    @Query("""
        select t from TestItem t
        where t.active = true
          and t.industryCode = :industry
          and t.grade = :grade
          and (t.specializationCode is null or t.specializationCode = :spec)
        """)
    List<TestItem> findForSession(
            @Param("industry") String industry,
            @Param("spec") String spec,
            @Param("grade") Grade grade
    );
}
