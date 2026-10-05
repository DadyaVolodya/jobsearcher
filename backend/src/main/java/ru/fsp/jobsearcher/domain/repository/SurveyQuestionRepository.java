package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.SurveyQuestion;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, UUID> {
    List<SurveyQuestion> findByQuestionnaireIdOrderBySortOrderAsc(UUID questionnaireId);
}
