package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.ChatThread;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {
    Optional<ChatThread> findByInvitationId(UUID invitationId);
    List<ChatThread> findByEmployerIdOrderByCreatedAtDesc(UUID employerId);
    List<ChatThread> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
