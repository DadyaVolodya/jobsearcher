package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.Questionnaire;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {
    Optional<Questionnaire> findByCode(String code);
    List<Questionnaire> findByActiveTrueAndAudience(String audience);
    List<Questionnaire> findByActiveTrueAndAudienceAndIndustryCode(String audience, String industryCode);
}
