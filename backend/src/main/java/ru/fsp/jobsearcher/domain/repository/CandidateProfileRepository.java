package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.enums.Grade;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByUserId(UUID userId);

    @Query("""
        select c from CandidateProfile c
        where c.published = true
          and c.categoryCode is not null
          and (:industry is null or c.industryCode = :industry)
          and (:spec is null or c.specializationCode = :spec)
          and (:grade is null or c.assignedGrade = :grade)
          and (:category is null or c.categoryCode = :category)
        """)
    List<CandidateProfile> searchPublished(
            @Param("industry") String industry,
            @Param("spec") String spec,
            @Param("grade") Grade grade,
            @Param("category") String category
    );
}
