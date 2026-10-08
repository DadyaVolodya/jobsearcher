package ru.fsp.jobsearcher.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.fsp.jobsearcher.api.common.ApiException;
import ru.fsp.jobsearcher.application.security.CurrentUser;
import ru.fsp.jobsearcher.consents.ConsentGate;
import ru.fsp.jobsearcher.domain.entity.CandidateProfile;
import ru.fsp.jobsearcher.domain.entity.FspAchievement;
import ru.fsp.jobsearcher.domain.enums.Grade;
import ru.fsp.jobsearcher.domain.enums.UserRole;
import ru.fsp.jobsearcher.domain.repository.CandidateProfileRepository;
import ru.fsp.jobsearcher.domain.repository.FspAchievementRepository;
import ru.fsp.jobsearcher.domain.repository.UserConsentRepository;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final UserConsentRepository userConsentRepository;
    private final FspAchievementRepository fspAchievementRepository;

    @Transactional(readOnly = true)
    public CandidateProfile requireMine(CurrentUser user) {
        if (user.role() != UserRole.CANDIDATE && user.role() != UserRole.ADMIN) {
            throw ApiException.forbidden("Только кандидат");
        }
        return candidateProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> ApiException.notFound("Профиль кандидата не найден"));
    }

    @Transactional
    public CandidateProfile updateProfile(CurrentUser user, UpdateRequest req) {
        CandidateProfile p = requireMine(user);
        if (req.fullName() != null) {
            p.setFullName(req.fullName());
        }
        if (req.phone() != null) {
            p.setPhone(req.phone());
        }
        if (req.city() != null) {
            p.setCity(req.city());
        }
        if (req.about() != null) {
            p.setAbout(req.about());
        }
        if (req.stack() != null) {
            p.setStack(req.stack());
        }
        if (req.softSkills() != null) {
            p.setSoftSkills(req.softSkills());
        }
        if (req.experienceYears() != null) {
            p.setExperienceYears(req.experienceYears());
        }
        if (req.resumeText() != null) {
            p.setResumeText(req.resumeText());
        }
        if (req.privacyHideContacts() != null) {
            p.setPrivacyHideContacts(req.privacyHideContacts());
        }
        if (req.claimedGrade() != null) {
            p.setClaimedGrade(Grade.from(req.claimedGrade()));
        }
        if (req.industryCode() != null) {
            p.setIndustryCode(req.industryCode());
        }
        if (req.specializationCode() != null) {
            p.setSpecializationCode(req.specializationCode());
        }
        if (req.fspParticipantId() != null) {
            p.setFspParticipantId(req.fspParticipantId().isBlank() ? null : req.fspParticipantId());
        }
        p.touch();
        return candidateProfileRepository.save(p);
    }

    @Transactional
    public CandidateProfile publish(CurrentUser user, boolean published) {
        CandidateProfile p = requireMine(user);
        if (published) {
            ConsentGate.requireForPublication(userConsentRepository.findByUserId(user.userId()));
            if (p.getCategoryCode() == null) {
                throw ApiException.badState("Сначала пройдите опрос и тестирование для присвоения категории");
            }
        }
        p.setPublished(published);
        p.touch();
        return candidateProfileRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<FspAchievement> achievements(UUID candidateId) {
        return fspAchievementRepository.findByCandidateId(candidateId);
    }

    @Transactional
    public FspAchievement linkFspStub(
            CurrentUser user,
            String fspId,
            String title,
            String eventName,
            Integer place,
            int points,
            String fspGrade
    ) {
        CandidateProfile p = requireMine(user);
        p.setFspParticipantId(fspId);
        if (fspGrade != null && !fspGrade.isBlank()) {
            p.setFspGrade(Grade.from(fspGrade));
        }
        p.touch();
        candidateProfileRepository.save(p);
        FspAchievement a = new FspAchievement();
        a.setId(UUID.randomUUID());
        a.setCandidateId(p.getId());
        a.setFspParticipantId(fspId);
        a.setTitle(title == null ? "Достижение ФСП" : title);
        a.setEventName(eventName);
        a.setPlace(place);
        a.setPoints(Math.max(0, points));
        a.setGradeHint(fspGrade == null || fspGrade.isBlank() ? null : Grade.from(fspGrade).name());
        a.setVerified(true);
        return fspAchievementRepository.save(a);
    }

    @Transactional(readOnly = true)
    public FspSummary fspSummary(CurrentUser user) {
        CandidateProfile p = requireMine(user);
        List<FspAchievement> list = fspAchievementRepository.findByCandidateId(p.getId());
        int points = list.stream().mapToInt(FspAchievement::getPoints).sum();
        return new FspSummary(
                p.getFspParticipantId(),
                p.getFspGrade() == null ? null : p.getFspGrade().name(),
                points,
                list.size(),
                list
        );
    }

    public record FspSummary(
            String fspParticipantId,
            String fspGrade,
            int totalPoints,
            int achievementsCount,
            List<FspAchievement> achievements
    ) {
    }

    public record UpdateRequest(
            String fullName,
            String phone,
            String city,
            String about,
            List<String> stack,
            List<String> softSkills,
            BigDecimal experienceYears,
            String resumeText,
            Boolean privacyHideContacts,
            String claimedGrade,
            String industryCode,
            String specializationCode,
            String fspParticipantId
    ) {
    }
}
