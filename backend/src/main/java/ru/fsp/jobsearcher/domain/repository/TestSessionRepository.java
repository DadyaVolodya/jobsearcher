package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.TestSession;

public interface TestSessionRepository extends JpaRepository<TestSession, UUID> {
    List<TestSession> findByCandidateIdOrderByStartedAtDesc(UUID candidateId);
}
