package ru.fsp.jobsearcher.domain.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.GradeChangeLog;

public interface GradeChangeLogRepository extends JpaRepository<GradeChangeLog, UUID> {
}
