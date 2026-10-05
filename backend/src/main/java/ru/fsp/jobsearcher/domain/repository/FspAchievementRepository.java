package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.FspAchievement;

public interface FspAchievementRepository extends JpaRepository<FspAchievement, UUID> {
    List<FspAchievement> findByCandidateId(UUID candidateId);
}
