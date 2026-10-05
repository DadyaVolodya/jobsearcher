package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.SurveySession;

public interface SurveySessionRepository extends JpaRepository<SurveySession, UUID> {
    List<SurveySession> findByUserIdOrderByStartedAtDesc(UUID userId);
}
