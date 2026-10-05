package ru.fsp.jobsearcher.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.fsp.jobsearcher.domain.entity.Invitation;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    List<Invitation> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
    List<Invitation> findByEmployerIdOrderByCreatedAtDesc(UUID employerId);
}
