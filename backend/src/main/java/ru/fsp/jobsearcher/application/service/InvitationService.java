package ru.fsp.jobsearcher.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.domain.entity.AppUser;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;
import ru.fsp.jobsearcher.domain.entity.Invitation;
import ru.fsp.jobsearcher.domain.enums.InvitationStatus;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.AppUserRepository;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.EmployerProfileRepository;
import ru.fsp.jobsearcher.domain.repository.InvitationRepository;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EmployerService employerService;
    private final CandidateService candidateService;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public Invitation create(CurrentUser user, CreateRequest req) {
        EmployerProfile employer = employerService.requireMine(user);
        CandidateProfile candidate = candidateProfileRepository.findById(req.candidateId())
                .orElseThrow(() -> ApiException.notFound("Кандидат не найден"));
        if (!candidate.isPublished() || candidate.getCategoryCode() == null) {
            throw ApiException.badState("Кандидат недоступен для приглашения");
        }
        if (req.salaryFrom() <= 0 || req.salaryTo() < req.salaryFrom()) {
            throw ApiException.badState("Зарплата обязательна: диапазон от–до в рублях");
        }
        Invitation inv = new Invitation();
        inv.setId(UUID.randomUUID());
        inv.setEmployerId(employer.getId());
        inv.setCandidateId(candidate.getId());
        inv.setNeedId(req.needId());
        inv.setMessage(req.message());
        inv.setSalaryFrom(req.salaryFrom());
        inv.setSalaryTo(req.salaryTo());
        inv.setStatus(InvitationStatus.SENT);
        Instant now = Instant.now();
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        return invitationRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public List<InvitationView> listForCurrent(CurrentUser user) {
        if (user.role() == UserRole.CANDIDATE) {
            CandidateProfile c = candidateService.requireMine(user);
            return invitationRepository.findByCandidateIdOrderByCreatedAtDesc(c.getId()).stream()
                    .map(i -> toView(i, false))
                    .toList();
        }
        EmployerProfile e = employerService.requireMine(user);
        return invitationRepository.findByEmployerIdOrderByCreatedAtDesc(e.getId()).stream()
                .map(i -> toView(i, i.getStatus() == InvitationStatus.ACCEPTED))
                .toList();
    }

    @Transactional
    public InvitationView updateStatus(CurrentUser user, UUID id, InvitationStatus status) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Приглашение не найдено"));
        CandidateProfile c = candidateService.requireMine(user);
        if (!inv.getCandidateId().equals(c.getId())) {
            throw ApiException.forbidden("Чужое приглашение");
        }
        if (status != InvitationStatus.VIEWED
                && status != InvitationStatus.ACCEPTED
                && status != InvitationStatus.DECLINED) {
            throw ApiException.badState("Недопустимый статус");
        }
        inv.setStatus(status);
        inv.setUpdatedAt(Instant.now());
        invitationRepository.save(inv);
        return toView(inv, status == InvitationStatus.ACCEPTED);
    }

    private InvitationView toView(Invitation inv, boolean revealCandidateContacts) {
        EmployerProfile employer = employerProfileRepository.findById(inv.getEmployerId())
                .orElseThrow(() -> ApiException.notFound("Работодатель не найден"));
        CandidateProfile candidate = candidateProfileRepository.findById(inv.getCandidateId())
                .orElseThrow(() -> ApiException.notFound("Кандидат не найден"));
        String email = null;
        String phone = null;
        if (revealCandidateContacts) {
            AppUser candUser = appUserRepository.findById(candidate.getUserId()).orElse(null);
            email = candUser == null ? null : candUser.getEmail();
            phone = candidate.getPhone();
        }
        return new InvitationView(
                inv.getId(),
                employer.getId(),
                employer.getCompanyName(),
                candidate.getId(),
                candidate.getFullName(),
                email,
                phone,
                inv.getNeedId(),
                inv.getMessage(),
                inv.getSalaryFrom(),
                inv.getSalaryTo(),
                inv.getStatus(),
                inv.getCreatedAt()
        );
    }

    public record CreateRequest(UUID candidateId, UUID needId, String message, int salaryFrom, int salaryTo) {
    }

    public record InvitationView(
            UUID id,
            UUID employerId,
            String companyName,
            UUID candidateId,
            String candidateName,
            String candidateEmail,
            String candidatePhone,
            UUID needId,
            String message,
            int salaryFrom,
            int salaryTo,
            InvitationStatus status,
            Instant createdAt
    ) {
    }
}
