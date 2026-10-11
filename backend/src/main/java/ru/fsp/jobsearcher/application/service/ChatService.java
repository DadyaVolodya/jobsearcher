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
import ru.fsp.jobsearcher.domain.entity.ChatMessage;
import ru.fsp.jobsearcher.domain.entity.ChatThread;
import ru.fsp.jobsearcher.domain.entity.EmployerProfile;
import ru.fsp.jobsearcher.domain.entity.Invitation;
import ru.fsp.jobsearcher.domain.enums.InvitationStatus;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.AppUserRepository;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.ChatMessageRepository;
import ru.fsp.jobsearcher.domain.repository.ChatThreadRepository;
import ru.fsp.jobsearcher.domain.repository.EmployerProfileRepository;
import ru.fsp.jobsearcher.domain.repository.InvitationRepository;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final InvitationRepository invitationRepository;
    private final EmployerService employerService;
    private final CandidateService candidateService;
    private final EmployerProfileRepository employerProfileRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public ChatThread ensureThreadForInvitation(Invitation invitation) {
        return chatThreadRepository.findByInvitationId(invitation.getId()).orElseGet(() -> {
            ChatThread t = new ChatThread();
            t.setId(UUID.randomUUID());
            t.setEmployerId(invitation.getEmployerId());
            t.setCandidateId(invitation.getCandidateId());
            t.setInvitationId(invitation.getId());
            t.setVacancyId(invitation.getVacancyId());
            t.setCreatedAt(Instant.now());
            return chatThreadRepository.save(t);
        });
    }

    @Transactional(readOnly = true)
    public List<ThreadView> list(CurrentUser user) {
        if (user.role() == UserRole.CANDIDATE) {
            CandidateProfile c = candidateService.requireMine(user);
            return chatThreadRepository.findByCandidateIdOrderByCreatedAtDesc(c.getId()).stream()
                    .map(t -> toView(t, user))
                    .toList();
        }
        EmployerProfile e = employerService.requireMine(user);
        return chatThreadRepository.findByEmployerIdOrderByCreatedAtDesc(e.getId()).stream()
                .map(t -> toView(t, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadSummary unreadSummary(CurrentUser user) {
        List<ThreadView> threads = list(user);
        int total = threads.stream().mapToInt(ThreadView::unreadCount).sum();
        return new UnreadSummary(total, threads.stream()
                .filter(t -> t.unreadCount() > 0)
                .map(t -> new UnreadThread(t.id(), t.unreadCount()))
                .toList());
    }

    @Transactional
    public List<ChatMessage> messages(CurrentUser user, UUID threadId) {
        ChatThread thread = requireAccess(user, threadId);
        markRead(user, thread);
        return chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
    }

    @Transactional
    public ChatMessage send(CurrentUser user, UUID threadId, String body) {
        if (body == null || body.isBlank()) {
            throw ApiException.badState("Пустое сообщение");
        }
        ChatThread thread = requireAccess(user, threadId);
        Invitation inv = invitationRepository.findById(thread.getInvitationId())
                .orElseThrow(() -> ApiException.notFound("Приглашение не найдено"));
        if (inv.getStatus() == InvitationStatus.DECLINED) {
            throw ApiException.badState("Диалог закрыт: приглашение отклонено");
        }
        ChatMessage msg = new ChatMessage();
        msg.setId(UUID.randomUUID());
        msg.setThreadId(thread.getId());
        msg.setSenderUserId(user.userId());
        msg.setBody(body.trim());
        msg.setCreatedAt(Instant.now());
        ChatMessage saved = chatMessageRepository.save(msg);
        markRead(user, thread);
        return saved;
    }

    private void markRead(CurrentUser user, ChatThread thread) {
        Instant now = Instant.now();
        if (user.role() == UserRole.CANDIDATE) {
            thread.setCandidateLastReadAt(now);
        } else {
            thread.setEmployerLastReadAt(now);
        }
        chatThreadRepository.save(thread);
    }

    private ChatThread requireAccess(CurrentUser user, UUID threadId) {
        ChatThread thread = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> ApiException.notFound("Чат не найден"));
        if (user.role() == UserRole.CANDIDATE) {
            CandidateProfile c = candidateService.requireMine(user);
            if (!thread.getCandidateId().equals(c.getId())) {
                throw ApiException.forbidden("Чужой чат");
            }
        } else {
            EmployerProfile e = employerService.requireMine(user);
            if (!thread.getEmployerId().equals(e.getId())) {
                throw ApiException.forbidden("Чужой чат");
            }
        }
        return thread;
    }

    private int unreadFor(ChatThread t, CurrentUser viewer) {
        Instant since = viewer.role() == UserRole.CANDIDATE
                ? t.getCandidateLastReadAt()
                : t.getEmployerLastReadAt();
        // Epoch instead of null: Postgres cannot infer type for "? is null" bind.
        Instant after = since == null ? Instant.EPOCH : since;
        return (int) chatMessageRepository.countUnread(t.getId(), viewer.userId(), after);
    }

    private ThreadView toView(ChatThread t, CurrentUser viewer) {
        Invitation inv = invitationRepository.findById(t.getInvitationId()).orElse(null);
        EmployerProfile employer = employerProfileRepository.findById(t.getEmployerId()).orElse(null);
        CandidateProfile candidate = candidateProfileRepository.findById(t.getCandidateId()).orElse(null);
        boolean reveal = inv != null && inv.getStatus() == InvitationStatus.ACCEPTED;
        String candEmail = null;
        String candPhone = null;
        if (reveal && candidate != null) {
            AppUser u = appUserRepository.findById(candidate.getUserId()).orElse(null);
            candEmail = u == null ? null : u.getEmail();
            candPhone = candidate.getPhone();
        }
        return new ThreadView(
                t.getId(),
                t.getInvitationId(),
                t.getVacancyId(),
                employer == null ? null : employer.getCompanyName(),
                candidate == null ? null : candidate.getFullName(),
                inv == null ? null : inv.getStatus(),
                inv == null ? 0 : inv.getSalaryFrom(),
                inv == null ? 0 : inv.getSalaryTo(),
                inv == null ? null : inv.getDeclineReason(),
                reveal,
                candEmail,
                candPhone,
                reveal ? (employer == null ? null : employer.getContactEmail()) : null,
                reveal ? (employer == null ? null : employer.getContactPhone()) : null,
                unreadFor(t, viewer),
                t.getCreatedAt()
        );
    }

    public record UnreadSummary(int unreadTotal, List<UnreadThread> threads) {
    }

    public record UnreadThread(UUID threadId, int unreadCount) {
    }

    public record ThreadView(
            UUID id,
            UUID invitationId,
            UUID vacancyId,
            String companyName,
            String candidateName,
            InvitationStatus invitationStatus,
            int salaryFrom,
            int salaryTo,
            String declineReason,
            boolean contactsRevealed,
            String candidateEmail,
            String candidatePhone,
            String employerEmail,
            String employerPhone,
            int unreadCount,
            Instant createdAt
    ) {
    }
}
